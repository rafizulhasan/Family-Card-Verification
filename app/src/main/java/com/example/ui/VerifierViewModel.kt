package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FamilyCardVerificationPipeline
import com.example.data.PdfRendererHelper
import com.example.domain.FamilyCardValidator
import com.example.model.PdfQueueItem
import com.example.model.QueueFilterMode
import com.example.model.QueueItemStatus
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VerifierUiState(
    val queueItems: List<PdfQueueItem> = emptyList(),
    val selectedItemId: String? = null,
    val activeFilter: QueueFilterMode = QueueFilterMode.ALL,
    val selectedDetailTab: Int = 0,
    val isQueueRunning: Boolean = false
) {
    val selectedItem: PdfQueueItem?
        get() = queueItems.firstOrNull { it.id == selectedItemId }

    val filteredQueueItems: List<PdfQueueItem>
        get() = when (activeFilter) {
            QueueFilterMode.ALL -> queueItems
            QueueFilterMode.WITH_WARNINGS -> queueItems.filter {
                it.status == QueueItemStatus.COMPLETED && (it.report?.warningCount ?: 0) > 0
            }
            QueueFilterMode.CONSISTENT -> queueItems.filter {
                it.status == QueueItemStatus.COMPLETED && (it.report?.isAllConsistent == true)
            }
            QueueFilterMode.FAILED -> queueItems.filter {
                it.status == QueueItemStatus.FAILED
            }
        }
}

class VerifierViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VerifierUiState())
    val uiState: StateFlow<VerifierUiState> = _uiState.asStateFlow()

    private var queueProcessorJob: Job? = null

    fun enqueuePdfUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val appContext = context.applicationContext
        val newItems = uris.map { uri ->
            val (name, size) = resolveFileMeta(appContext, uri)
            PdfQueueItem(
                id = UUID.randomUUID().toString(),
                uri = uri,
                fileName = name,
                fileSizeBytes = size,
                status = QueueItemStatus.PENDING
            )
        }
        _uiState.update { state ->
            val updatedList = state.queueItems + newItems
            state.copy(
                queueItems = updatedList,
                selectedItemId = state.selectedItemId ?: newItems.first().id
            )
        }
        startSequentialQueueProcessing(appContext)
    }

    fun selectQueueItem(itemId: String?) {
        _uiState.update { it.copy(selectedItemId = itemId, selectedDetailTab = 0) }
    }

    fun selectDetailTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedDetailTab = tabIndex) }
    }

    fun setQueueFilter(mode: QueueFilterMode) {
        _uiState.update { it.copy(activeFilter = mode) }
    }

    fun removeQueueItem(itemId: String) {
        _uiState.update { state ->
            val remaining = state.queueItems.filterNot { it.id == itemId }
            val nextSelected = if (state.selectedItemId == itemId) {
                remaining.firstOrNull()?.id
            } else {
                state.selectedItemId
            }
            state.copy(queueItems = remaining, selectedItemId = nextSelected)
        }
    }

    fun clearAllQueue() {
        queueProcessorJob?.cancel()
        _uiState.update { VerifierUiState() }
    }

    fun retrySingleItem(context: Context, itemId: String) {
        val appContext = context.applicationContext
        _uiState.update { state ->
            state.copy(
                queueItems = state.queueItems.map { item ->
                    if (item.id == itemId) {
                        item.copy(
                            status = QueueItemStatus.PENDING,
                            errorMessage = null,
                            progressMessage = ""
                        )
                    } else {
                        item
                    }
                }
            )
        }
        startSequentialQueueProcessing(appContext)
    }

    fun retryAllFailed(context: Context) {
        val appContext = context.applicationContext
        _uiState.update { state ->
            state.copy(
                queueItems = state.queueItems.map { item ->
                    if (item.status == QueueItemStatus.FAILED) {
                        item.copy(
                            status = QueueItemStatus.PENDING,
                            errorMessage = null,
                            progressMessage = ""
                        )
                    } else {
                        item
                    }
                }
            )
        }
        startSequentialQueueProcessing(appContext)
    }

    private fun startSequentialQueueProcessing(appContext: Context) {
        if (queueProcessorJob?.isActive == true) return

        queueProcessorJob = viewModelScope.launch {
            _uiState.update { it.copy(isQueueRunning = true) }
            try {
                while (true) {
                    val nextPending = _uiState.value.queueItems.firstOrNull {
                        it.status == QueueItemStatus.PENDING
                    } ?: break

                    processSingleQueueItem(appContext, nextPending)
                }
            } finally {
                _uiState.update { it.copy(isQueueRunning = false) }
            }
        }
    }

    private suspend fun processSingleQueueItem(appContext: Context, item: PdfQueueItem) {
        updateItemState(item.id) {
            it.copy(
                status = QueueItemStatus.PROCESSING,
                progressMessage = "বাংলা টেক্সট এক্সট্রাকশন ও ক্রাইটেরিয়া যাচাই চলছে…",
                errorMessage = null
            )
        }

        try {
            val pdfBytes = withContext(Dispatchers.IO) {
                item.uri?.let { uri ->
                    appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
            } ?: throw IllegalArgumentException("পিডিএফ ফাইলটি পড়া সম্ভব হয়নি।")

            val outcome = FamilyCardVerificationPipeline.extractWithAutomaticOcrFallback(
                context = appContext,
                pdfBytes = pdfBytes,
                onStartingOcrFallback = { totalPages ->
                    updateItemState(item.id) {
                        it.copy(
                            progressMessage = "২.৫x স্কেলে Tesseract (ben+eng) OCR শুরু হচ্ছে ($totalPages পৃষ্ঠা)…"
                        )
                    }
                },
                onOcrPageProgress = { current, total ->
                    updateItemState(item.id) {
                        it.copy(
                            progressMessage = "Tesseract (ben+eng) OCR চলছে: পৃষ্ঠা $current / $total…"
                        )
                    }
                }
            )

            val report = FamilyCardValidator.buildVerificationReport(outcome)
            val previewBitmaps = PdfRendererHelper.renderPreviewPages(appContext, pdfBytes)

            updateItemState(item.id) {
                it.copy(
                    status = QueueItemStatus.COMPLETED,
                    report = report,
                    pageBitmaps = previewBitmaps,
                    progressMessage = "",
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            updateItemState(item.id) {
                it.copy(
                    status = QueueItemStatus.FAILED,
                    progressMessage = "",
                    errorMessage = e.message ?: "পিডিএফ যাচাইকরণে ত্রুটি ঘটেছে।"
                )
            }
        }
    }

    private fun updateItemState(itemId: String, transform: (PdfQueueItem) -> PdfQueueItem) {
        _uiState.update { state ->
            state.copy(
                queueItems = state.queueItems.map { item ->
                    if (item.id == itemId) transform(item) else item
                }
            )
        }
    }

    private fun resolveFileMeta(context: Context, uri: Uri): Pair<String, Long> {
        var name = "family_card_application.pdf"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return name to size
    }
}
