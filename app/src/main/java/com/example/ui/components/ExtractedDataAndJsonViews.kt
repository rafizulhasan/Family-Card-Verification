package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ExtractedFamilyCardData
import com.example.model.FamilyMemberInfo

@Composable
fun ExtractedDataView(
    data: ExtractedFamilyCardData,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("extracted_data_view")
    ) {
        SectionCard(
            title = "কার্যক্রমের তথ্য ও ব্যক্তিগত তথ্য",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            KeyValueRow("আবেদন আইডি", data.applicant.applicationId)
            KeyValueRow("আবেদনের তারিখ", data.applicant.applicationDate)
            KeyValueRow("প্রধান কার্যক্রম", data.programInfo.mainProgram)
            KeyValueRow("উপ-কার্যক্রম", data.programInfo.subProgram)
            KeyValueRow("পরিবারের সদস্য সংখ্যা", data.programInfo.familyMemberCount)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            KeyValueRow("নাম (বাংলা)", data.applicant.nameBangla)
            KeyValueRow("নাম (ইংরেজি)", data.applicant.nameEnglish)
            KeyValueRow("লিঙ্গ", data.applicant.gender)
            KeyValueRow("এনআইডি নম্বর", data.applicant.nid)
            KeyValueRow("জাতীয়তা", data.applicant.nationality)
            KeyValueRow("মোবাইল", data.applicant.mobile)
            KeyValueRow("ইমেইল", data.applicant.email)
            KeyValueRow("জন্ম তারিখ", data.applicant.dateOfBirth)
            KeyValueRow("বয়স", data.applicant.age)
            KeyValueRow("পিতার নাম (বাংলা)", data.applicant.fatherNameBangla)
            KeyValueRow("পিতার নাম (ইংরেজি)", data.applicant.fatherNameEnglish)
            KeyValueRow("মাতার নাম (বাংলা)", data.applicant.motherNameBangla)
            KeyValueRow("মাতার নাম (ইংরেজি)", data.applicant.motherNameEnglish)
            KeyValueRow("বৈবাহিক অবস্থা", data.applicant.maritalStatus)
            KeyValueRow("ধর্ম", data.applicant.religion)
        }

        SectionCard(
            title = "স্থায়ী ঠিকানা ও তথ্য সংগ্রহের ঠিকানা",
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            KeyValueRow("স্থায়ী ঠিকানা", data.applicant.permanentAddress)
            KeyValueRow("তথ্য সংগ্রহের ঠিকানা", data.applicant.presentAddress)
        }

        if (data.bankMfsInfo.bankOrMfsName.isNotBlank() || data.bankMfsInfo.accountType.isNotBlank()) {
            SectionCard(
                title = "ব্যাংক / এমএফএস তথ্য",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                KeyValueRow("হিসাবের ধরন", data.bankMfsInfo.accountType)
                KeyValueRow("ব্যাংক", data.bankMfsInfo.bankOrMfsName)
                KeyValueRow("শাখা", data.bankMfsInfo.branchName)
                KeyValueRow("হিসাবের নাম", data.bankMfsInfo.accountHolderName)
                KeyValueRow("হিসাব নম্বর", data.bankMfsInfo.accountNumber)
                KeyValueRow("হিসাবের মালিক", data.bankMfsInfo.accountOwnerRelation)
            }
        }

        SectionCard(
            title = "পিএমটি (PMT) ও অতিরিক্ত ভাতার তথ্য",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            KeyValueRow("খানার সদস্য সংখ্যা", data.householdInfo.householdMemberCount)
            KeyValueRow("খানার শিশু সংখ্যা", data.householdInfo.childCount)
            KeyValueRow("খানার প্রধানের বয়স", data.householdInfo.householdHeadAgeRange)
            KeyValueRow("খানা প্রধানের শিক্ষা", data.householdInfo.householdHeadEducation)
            KeyValueRow("স্বামীর শিক্ষা", data.householdInfo.spouseEducation)
            KeyValueRow("কৃষি/অকৃষি শ্রমিক", data.householdInfo.agriOrNonAgriDayLaborerStatus)
            KeyValueRow("খানায় মোট জমির পরিমাণ", data.householdInfo.landOwnershipDecimal)
            KeyValueRow("গত ২ বছরে বৈদেশিক অর্থ গ্রহণ", data.householdInfo.receivedRemittanceLast2Years)
            KeyValueRow("প্রধান ঘরের দেয়াল", data.householdInfo.mainWallMaterial)
            KeyValueRow("পৃথক খাবার ঘর আছে কি", data.householdInfo.hasSeparateDiningRoom)
            KeyValueRow("পৃথক রান্নাঘর আছে কি", data.householdInfo.hasSeparateKitchen)
            KeyValueRow("খাবার পানির উৎস", data.householdInfo.drinkingWaterSource)
            KeyValueRow("পায়খানা/টয়লেটের ধরন", data.householdInfo.toiletType)
            KeyValueRow("মালিকানাধীন পরিসম্পদ", data.householdInfo.ownedAssetsRaw)
            KeyValueRow("প্রধান ঘরের ছাদ", data.householdInfo.mainRoofMaterial)
            KeyValueRow("মোট রুম/কক্ষ সংখ্যা", data.householdInfo.totalRoomsCount)
            KeyValueRow("বাসস্থানটি কোথায় অবস্থিত", data.householdInfo.residenceAreaType)
            KeyValueRow("বসতঘরের মেঝে বা ফ্লোর", data.householdInfo.mainFloorMaterial)
            KeyValueRow("রান্নার জ্বালানির উপকরণ", data.householdInfo.mainCookingFuel)
            KeyValueRow("বিদ্যুৎ লাইনের উৎস", data.householdInfo.electricitySource)
            KeyValueRow("ভূমির/বসতঘরের মালিকানা ধরণ", data.householdInfo.landOrHouseOwnershipType)
            KeyValueRow("ক্ষুদ্র নৃগোষ্ঠী কি না", data.householdInfo.isEthnicMinority)
            KeyValueRow("মাসিক এলপিজি গ্যাস ব্যবহার", data.householdInfo.monthlyLpgUsageKg)
            KeyValueRow("কৃষিতে মাসিক ডিজেল ব্যবহার", data.householdInfo.monthlyDieselUsageKg)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            KeyValueRow("পরিবারে পুরুষ সদস্য সংখ্যা", data.additionalAllowanceInfo.maleMemberCount)
            KeyValueRow("পরিবারে নারী সদস্য সংখ্যা", data.additionalAllowanceInfo.femaleMemberCount)
            KeyValueRow("পরিবারে হিজড়া সদস্য সংখ্যা", data.additionalAllowanceInfo.hijraMemberCount)
            KeyValueRow("ডিএসএস প্রশিক্ষণ/আর্থিক সুবিধা", data.additionalAllowanceInfo.receivedDssTrainingOrBenefit)
            KeyValueRow("মোবাইল ব্যাংকিং হিসাব", data.additionalAllowanceInfo.mobileBankingAccountType)
            KeyValueRow("বিদেশে বসবাসরত পুরুষ সদস্য", data.additionalAllowanceInfo.abroadMaleCount)
            KeyValueRow("বিদেশে বসবাসরত মহিলা সদস্য", data.additionalAllowanceInfo.abroadFemaleCount)
            KeyValueRow("বিদেশে বসবাসরত হিজড়া সদস্য", data.additionalAllowanceInfo.abroadHijraCount)
        }

        SectionCard(
            title = "পরিবারের সদস্যদের তালিকা (${data.familyMembers.size} জন)",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            if (data.familyMembers.isEmpty()) {
                Text(
                    text = "সদস্য তালিকার কোনো তথ্য পাওয়া যায়নি।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    data.familyMembers.forEach { member ->
                        FamilyMemberRowCard(member)
                    }
                }
            }
        }

        if (data.fundUsageItems.isNotEmpty() || data.totalFundUsageTaka.isNotBlank()) {
            SectionCard(
                title = "অর্থ ব্যবহার (মোট: ${data.totalFundUsageTaka.ifBlank { "—" }} টাকা)",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                data.fundUsageItems.forEach { item ->
                    KeyValueRow(item.sector, "${item.amountTaka} টাকা (${item.remarks.ifBlank { "—" }})")
                }
                KeyValueRow("মোট", "${data.totalFundUsageTaka} টাকা")
            }
        }

        if (data.entryOperatorInfo.name.isNotBlank() || data.entryOperatorInfo.userId.isNotBlank()) {
            SectionCard(
                title = "এন্ট্রি ব্যবহারকারীর তথ্য",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AssignmentInd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                KeyValueRow("এন্ট্রি ব্যবহারকারীর নাম", data.entryOperatorInfo.name)
                KeyValueRow("এন্ট্রি ব্যবহারকারীর মোবাইল", data.entryOperatorInfo.mobile)
                KeyValueRow("ইউজার আইডি", data.entryOperatorInfo.userId)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.48f)
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.52f)
        )
    }
}

@Composable
private fun FamilyMemberRowCard(member: FamilyMemberInfo) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = member.headerTitle.ifBlank {
                    "সদস্য #${member.serial}: ${member.nameBangla.ifBlank { "নাম অপ্রাপ্য" }}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueRow("নাম (বাংলা)", member.nameBangla)
            if (member.nameEnglish.isNotBlank()) {
                KeyValueRow("নাম (ইংরেজি)", member.nameEnglish)
            }
            KeyValueRow("সম্পর্ক", member.relationWithApplicant)
            KeyValueRow("এনআইডি / জন্ম নিবন্ধন", member.nidOrBirthReg)
            KeyValueRow("জন্ম তারিখ", member.dateOfBirthOrAge)
            if (member.fatherNameBangla.isNotBlank()) {
                KeyValueRow("পিতার নাম (বাংলা)", member.fatherNameBangla)
            }
            if (member.motherNameBangla.isNotBlank()) {
                KeyValueRow("মাতার নাম (বাংলা)", member.motherNameBangla)
            }
            KeyValueRow("লিঙ্গ", member.gender)
            KeyValueRow("বৈবাহিক অবস্থা", member.maritalStatus)
            KeyValueRow("জীবিকার পেশা", member.livelihoodProfession.ifBlank { member.profession })
            if (member.estimatedAnnualIncome.isNotBlank()) {
                KeyValueRow("আনুমানিক বার্ষিক আয়", member.estimatedAnnualIncome)
            }
            if (member.highestEducation.isNotBlank()) {
                KeyValueRow("শিক্ষাগত যোগ্যতা", member.highestEducation)
            }
            if (member.healthCondition.isNotBlank()) {
                KeyValueRow("স্বাস্থ্য অবস্থা", member.healthCondition)
            }
        }
    }
}

@Composable
fun JsonOutputView(
    formattedJson: String,
    onCopyJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("json_output_view")
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "স্ট্রাকচার্ড JSON আউটপুট",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onCopyJson,
                modifier = Modifier.testTag("copy_json_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.cd_copy_json)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = stringResource(R.string.btn_copy_json))
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            SelectionContainer {
                Text(
                    text = formattedJson,
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
fun PdfPagesPreviewList(
    pageBitmaps: List<Bitmap>,
    rawTextFallback: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_pages_preview_list")
    ) {
        if (pageBitmaps.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ডিজিটাল টেক্সট স্ট্রিম প্রিভিউ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = rawTextFallback.ifBlank { "কোনো পৃষ্ঠার প্রিভিউ পাওয়া যায়নি।" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            pageBitmaps.forEachIndexed { idx, bmp ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "পৃষ্ঠা ${idx + 1} / ${pageBitmaps.size}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_pdf_page),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
