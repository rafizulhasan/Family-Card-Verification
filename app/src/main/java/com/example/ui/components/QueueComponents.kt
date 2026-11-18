package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PdfQueueItem
import com.example.model.QueueFilterMode
import com.example.model.QueueItemStatus
import com.example.ui.theme.GovCrimsonAccent
import com.example.ui.theme.GovPassContainer
import com.example.ui.theme.GovPassGreen
import com.example.ui.theme.GovWarningAmber
import com.example.ui.theme.GovWarningContainer

@Composable
fun EmptyQueueCard(
    onPickPdfs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_queue_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.UploadFile,
                contentDescription = stringResource(R.string.cd_upload_pdf),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.empty_queue_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "সমাজসেবা অধিদফতরের পরিবার কার্ড আবেদনপত্র (এক বা একাধিক PDF) নির্বাচন করুন। অ্যাপটি ১০০% অফলাইনে বাংলা ইউনিকোড ডিকোড, পিএমটি ও সদস্য তথ্য এক্সট্রাকশন এবং নির্ধারিত শর্তসমূহ যাচাই করবে।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPickPdfs,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_pdfs_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.btn_pick_multiple_pdfs))
            }
        }
    }
}

@Composable
fun QueueOverviewToolbar(
    items: List<PdfQueueItem>,
    activeFilter: QueueFilterMode,
    onSelectFilter: (QueueFilterMode) -> Unit,
    onAddMorePdfs: () -> Unit,
    onRetryAllFailed: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = items.size
    val completed = items.count { it.status == QueueItemStatus.COMPLETED }
    val failed = items.count { it.status == QueueItemStatus.FAILED }
    val processing = items.any { it.status == QueueItemStatus.PROCESSING }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("queue_overview_toolbar"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "সিকোয়েন্সিয়াল কিউ প্রগ্রেস: $completed / $total সম্পন্ন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) completed.toFloat() / total.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                QueueFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = activeFilter == mode,
                        onClick = { onSelectFilter(mode) },
                        label = { Text(mode.labelBangla) },
                        modifier = Modifier.testTag("filter_chip_${mode.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onAddMorePdfs,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_more_pdfs_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_add_more_pdfs),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (failed > 0) {
                    OutlinedButton(
                        onClick = onRetryAllFailed,
                        modifier = Modifier.testTag("retry_failed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.btn_retry_all_failed))
                    }
                }

                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = stringResource(R.string.btn_clear_queue),
                        tint = GovCrimsonAccent
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemCard(
    item: PdfQueueItem,
    isSelected: Boolean,
    onSelectItem: () -> Unit,
    onRetryItem: () -> Unit,
    onRemoveItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderStrokeColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectItem() }
            .testTag("queue_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderStrokeColor
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val applicantName = item.report?.extractedData?.applicant?.nameBangla.orEmpty()
                        if (applicantName.isNotBlank()) {
                            Text(
                                text = "আবেদনকারী: $applicantName • আইডি: ${item.report?.extractedData?.applicant?.applicationId.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRemoveItem,
                    modifier = Modifier.testTag("remove_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_remove_queue_item)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (item.status) {
                    QueueItemStatus.PENDING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.queue_status_pending),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    QueueItemStatus.PROCESSING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.progressMessage.ifBlank { stringResource(R.string.queue_status_processing) },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    QueueItemStatus.COMPLETED -> {
                        val report = item.report
                        if (report != null) {
                            val isConsistent = report.isAllConsistent
                            val totalCriteria = report.criteriaResults.size
                            Surface(
                                color = if (isConsistent) GovPassContainer else GovWarningContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isConsistent) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (isConsistent) GovPassGreen else GovWarningAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isConsistent) {
                                            "${report.passCount}/${totalCriteria} সামঞ্জস্যপূর্ণ"
                                        } else {
                                            "${report.warningCount}টি অসঙ্গতির সতর্কবার্তা"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isConsistent) GovPassGreen else GovWarningAmber
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.btn_view_full_report),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    QueueItemStatus.FAILED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = GovCrimsonAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.errorMessage ?: stringResource(R.string.queue_status_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = GovCrimsonAccent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = onRetryItem) {
                            Text(text = stringResource(R.string.btn_reverify))
                        }
                    }
                }
            }
        }
    }
}
