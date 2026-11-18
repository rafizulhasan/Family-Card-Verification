package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CriterionCheckResult
import com.example.model.CriterionCheckStatus
import com.example.model.FamilyCardVerificationReport
import com.example.ui.theme.GovPassContainer
import com.example.ui.theme.GovPassGreen
import com.example.ui.theme.GovWarningAmber
import com.example.ui.theme.GovWarningContainer

@Composable
fun ReportSummaryBanner(
    report: FamilyCardVerificationReport,
    modifier: Modifier = Modifier
) {
    val isPass = report.isAllConsistent
    val containerColor = if (isPass) GovPassContainer else GovWarningContainer
    val accentColor = if (isPass) GovPassGreen else GovWarningAmber
    val totalCriteria = report.criteriaResults.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("report_summary_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isPass) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isPass) {
                                "সকল ${totalCriteria}টি ক্রাইটেরিয়া সামঞ্জস্যপূর্ণ"
                            } else {
                                "${totalCriteria}টির মধ্যে ${report.warningCount}টি ক্রাইটেরিয়ায় অসঙ্গতির সতর্কবার্তা"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "সামঞ্জস্যপূর্ণ: ${report.passCount}টি • সতর্কবার্তা: ${report.warningCount}টি • অক্ষর: ${report.extractedCharCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.75f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ইঞ্জিন: ${report.engineUsedLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

@Composable
fun CriterionResultCard(
    criterion: CriterionCheckResult,
    modifier: Modifier = Modifier
) {
    val isPass = criterion.status == CriterionCheckStatus.PASS
    val badgeBg = if (isPass) GovPassContainer else GovWarningContainer
    val badgeText = if (isPass) GovPassGreen else GovWarningAmber

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("criterion_card_${criterion.criterionNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(badgeBg, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPass) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = criterion.titleBangla,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isPass) {
                            stringResource(R.string.status_pass_badge)
                        } else {
                            stringResource(R.string.status_warning_badge)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = criterion.summaryBangla,
                style = MaterialTheme.typography.bodyLarge
            )

            if (criterion.comparedFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    criterion.comparedFields.forEach { (label, value) ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.48f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(0.52f)
                            )
                        }
                    }
                }
            }

            if (criterion.detailsBangla.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        criterion.detailsBangla.forEach { detail ->
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
