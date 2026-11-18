package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.FamilyCardVerificationReport
import com.example.model.PdfQueueItem
import com.example.model.QueueItemStatus
import com.example.ui.components.CriterionResultCard
import com.example.ui.components.EmptyQueueCard
import com.example.ui.components.ExtractedDataView
import com.example.ui.components.JsonOutputView
import com.example.ui.components.PdfPagesPreviewList
import com.example.ui.components.QueueItemCard
import com.example.ui.components.QueueOverviewToolbar
import com.example.ui.components.ReportSummaryBanner
import com.example.ui.theme.GovEmeraldPrimary
import com.example.ui.theme.GovEmeraldSecondary

@Composable
fun VerifierScreen(
    viewModel: VerifierViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.enqueuePdfUris(context, uris)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            GovernmentHeaderBanner()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("verifier_main_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.queueItems.isEmpty()) {
                item {
                    EmptyQueueCard(
                        onPickPdfs = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
                }
            } else {
                item {
                    QueueOverviewToolbar(
                        items = uiState.queueItems,
                        activeFilter = uiState.activeFilter,
                        onSelectFilter = viewModel::setQueueFilter,
                        onAddMorePdfs = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        onRetryAllFailed = {
                            viewModel.retryAllFailed(context)
                        },
                        onClearAll = viewModel::clearAllQueue
                    )
                }

                items(
                    items = uiState.filteredQueueItems,
                    key = { it.id }
                ) { queueItem ->
                    QueueItemCard(
                        item = queueItem,
                        isSelected = uiState.selectedItemId == queueItem.id,
                        onSelectItem = { viewModel.selectQueueItem(queueItem.id) },
                        onRetryItem = { viewModel.retrySingleItem(context, queueItem.id) },
                        onRemoveItem = { viewModel.removeQueueItem(queueItem.id) }
                    )
                }

                val selectedItem = uiState.selectedItem
                val selectedReport = selectedItem?.report
                if (selectedItem != null && selectedItem.status == QueueItemStatus.COMPLETED && selectedReport != null) {
                    item {
                        SelectedReportDetailHeader(
                            selectedItem = selectedItem,
                            allItems = uiState.queueItems,
                            onSelectRelative = { targetId -> viewModel.selectQueueItem(targetId) },
                            onShareReport = {
                                shareVerificationReport(context, selectedItem.fileName, selectedReport)
                            }
                        )
                    }

                    item {
                        ReportSummaryBanner(report = selectedReport)
                    }

                    item {
                        val tabs = listOf(
                            stringResource(R.string.tab_warnings),
                            stringResource(R.string.tab_extracted_data),
                            stringResource(R.string.tab_json),
                            stringResource(R.string.tab_pdf_pages)
                        )
                        ScrollableTabRow(
                            selectedTabIndex = uiState.selectedDetailTab,
                            edgePadding = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_detail_tabs")
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = uiState.selectedDetailTab == index,
                                    onClick = { viewModel.selectDetailTab(index) },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (uiState.selectedDetailTab == index) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    modifier = Modifier.testTag("detail_tab_$index")
                                )
                            }
                        }
                    }

                    when (uiState.selectedDetailTab) {
                        0 -> {
                            items(
                                items = selectedReport.criteriaResults,
                                key = { "crit_${it.criterionNumber}" }
                            ) { criterion ->
                                CriterionResultCard(criterion = criterion)
                            }
                        }

                        1 -> {
                            item {
                                ExtractedDataView(data = selectedReport.extractedData)
                            }
                        }

                        2 -> {
                            item {
                                JsonOutputView(
                                    formattedJson = selectedReport.formattedJson,
                                    onCopyJson = {
                                        copyJsonToClipboard(context, selectedReport.formattedJson)
                                    }
                                )
                            }
                        }

                        3 -> {
                            item {
                                PdfPagesPreviewList(
                                    pageBitmaps = selectedItem.pageBitmaps,
                                    rawTextFallback = selectedReport.extractedData.rawFullText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GovernmentHeaderBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(GovEmeraldPrimary, GovEmeraldSecondary)
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("government_header_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.header_ministry),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFD1FAE5)
                )
                Text(
                    text = stringResource(R.string.header_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun SelectedReportDetailHeader(
    selectedItem: PdfQueueItem,
    allItems: List<PdfQueueItem>,
    onSelectRelative: (String) -> Unit,
    onShareReport: () -> Unit
) {
    val currentIndex = allItems.indexOfFirst { it.id == selectedItem.id }
    val prevItem = if (currentIndex > 0) allItems[currentIndex - 1] else null
    val nextItem = if (currentIndex in 0 until allItems.size - 1) allItems[currentIndex + 1] else null

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { prevItem?.let { onSelectRelative(it.id) } },
                    enabled = prevItem != null,
                    modifier = Modifier.testTag("prev_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = stringResource(R.string.cd_prev_pdf)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "নির্বাচিত ফাইল (${currentIndex + 1}/${allItems.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = selectedItem.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { nextItem?.let { onSelectRelative(it.id) } },
                    enabled = nextItem != null,
                    modifier = Modifier.testTag("next_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = stringResource(R.string.cd_next_pdf)
                    )
                }
            }

            IconButton(
                onClick = onShareReport,
                modifier = Modifier.testTag("share_report_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.cd_share_report),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun copyJsonToClipboard(context: Context, jsonText: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("family_card_json", jsonText))
    Toast.makeText(
        context,
        context.getString(R.string.copied_json_toast),
        Toast.LENGTH_SHORT
    ).show()
}

private fun shareVerificationReport(
    context: Context,
    fileName: String,
    report: FamilyCardVerificationReport
) {
    val summaryBuilder = StringBuilder()
    summaryBuilder.appendLine("পরিবার কার্ড আবেদনপত্র যাচাইকরণ রিপোর্ট")
    summaryBuilder.appendLine("ফাইল: $fileName")
    summaryBuilder.appendLine("আবেদন আইডি: ${report.extractedData.applicant.applicationId}")
    summaryBuilder.appendLine("আবেদনকারী: ${report.extractedData.applicant.nameBangla}")
    summaryBuilder.appendLine("এনআইডি: ${report.extractedData.applicant.nid}")
    summaryBuilder.appendLine("ইঞ্জিন: ${report.engineUsedLabel}")
    summaryBuilder.appendLine("--------------------------------------------------")
    report.criteriaResults.forEach { c ->
        val badge = if (c.status.name == "PASS") "[সামঞ্জস্যপূর্ণ]" else "[অসঙ্গতির সতর্কবার্তা]"
        summaryBuilder.appendLine("${c.titleBangla} -> $badge")
        summaryBuilder.appendLine("সারসংক্ষেপ: ${c.summaryBangla}")
        summaryBuilder.appendLine()
    }
    summaryBuilder.appendLine("--- JSON আউটপুট ---")
    summaryBuilder.appendLine(report.formattedJson)

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "পরিবার কার্ড যাচাই রিপোর্ট - $fileName")
        putExtra(Intent.EXTRA_TEXT, summaryBuilder.toString())
    }
    context.startActivity(Intent.createChooser(sendIntent, "রিপোর্ট শেয়ার করুন"))
}
