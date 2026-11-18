package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class ProgramOverviewInfo(
    val mainProgram: String = "",
    val subProgram: String = "",
    val familyMemberCount: String = ""
)

data class ApplicantInfo(
    val applicationId: String = "",
    val applicationDate: String = "",
    val nameBangla: String = "",
    val nameEnglish: String = "",
    val nid: String = "",
    val nationality: String = "",
    val dateOfBirth: String = "",
    val age: String = "",
    val mobile: String = "",
    val email: String = "",
    val fatherNameBangla: String = "",
    val fatherNameEnglish: String = "",
    val motherNameBangla: String = "",
    val motherNameEnglish: String = "",
    val spouseName: String = "",
    val gender: String = "",
    val maritalStatus: String = "",
    val religion: String = "",
    val profession: String = "",
    val highestEducation: String = "",
    val presentAddress: String = "",
    val permanentAddress: String = ""
)

data class AddressDetails(
    val division: String = "",
    val district: String = "",
    val upazila: String = "",
    val unionName: String = "",
    val ward: String = "",
    val addressLine: String = "",
    val postCode: String = ""
) {
    fun toFormattedLine(): String {
        return listOf(addressLine, ward, unionName, upazila, district, division, postCode)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
    }
}

data class BankMfsInfo(
    val accountType: String = "",
    val bankOrMfsName: String = "",
    val branchName: String = "",
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val accountOwnerRelation: String = ""
)

data class AdditionalAllowanceInfo(
    val maleMemberCount: String = "",
    val femaleMemberCount: String = "",
    val hijraMemberCount: String = "",
    val receivedDssTrainingOrBenefit: String = "",
    val mobileBankingAccountType: String = "",
    val abroadMaleCount: String = "",
    val abroadFemaleCount: String = "",
    val abroadHijraCount: String = ""
) {
    val totalAbroadCount: Int
        get() = listOf(abroadMaleCount, abroadFemaleCount, abroadHijraCount).sumOf { raw ->
            raw.map { ch ->
                if (ch in '০'..'৯') ('0'.code + (ch.code - '০'.code)).toChar() else ch
            }.joinToString("").trim().toIntOrNull() ?: 0
        }
}

data class FamilyMemberInfo(
    val serial: Int = 1,
    val headerTitle: String = "",
    val nameBangla: String = "",
    val nameEnglish: String = "",
    val verificationType: String = "",
    val relationWithApplicant: String = "",
    val nidOrBirthReg: String = "",
    val dateOfBirthOrAge: String = "",
    val fatherNameBangla: String = "",
    val motherNameBangla: String = "",
    val gender: String = "",
    val maritalStatus: String = "",
    val religion: String = "",
    val mobile: String = "",
    val profession: String = "",
    val livelihoodProfession: String = "",
    val estimatedAnnualIncome: String = "",
    val isGovEmployee: String = "",
    val isDisabled: String = "",
    val disabilityType: String = "",
    val literacy: String = "",
    val isCurrentStudent: String = "",
    val highestEducation: String = "",
    val healthCondition: String = "",
    val underSocialSafetyNet: String = "",
    val receivedDssTrainingOrSupport: String = "",
    val govCardType: String = "",
    val isExpatriateOrAbroad: Boolean = false,
    val monthlyIncome: String = "",
    val rawRowText: String = ""
)

data class HouseholdSocioEconomicInfo(
    val householdMemberCount: String = "",
    val childCount: String = "",
    val householdHeadAgeRange: String = "",
    val householdHeadEducation: String = "",
    val spouseEducation: String = "",
    val agriOrNonAgriDayLaborerStatus: String = "",
    val landOwnershipDecimal: String = "",
    val receivedRemittanceLast2Years: String = "",
    val mainWallMaterial: String = "",
    val hasSeparateDiningRoom: String = "",
    val hasSeparateKitchen: String = "",
    val drinkingWaterSource: String = "",
    val toiletType: String = "",
    val ownedAssetsRaw: String = "",
    val detectedAssets: List<String> = emptyList(),
    val mainRoofMaterial: String = "",
    val totalRoomsCount: String = "",
    val residenceAreaType: String = "",
    val mainFloorMaterial: String = "",
    val mainCookingFuel: String = "",
    val electricitySource: String = "",
    val landOrHouseOwnershipType: String = "",
    val isEthnicMinority: String = "",
    val monthlyLpgUsageKg: String = "",
    val monthlyDieselUsageKg: String = "",
    val primaryLivelihoodProfession: String = "",
    val hasExpatriateMemberField: String = "",
    val remittanceOrAbroadDetails: String = "",
    val houseType: String = "",
    val monthlyFamilyIncome: String = ""
)

data class FundUsageItem(
    val sector: String = "",
    val amountTaka: String = "",
    val remarks: String = ""
)

data class EntryOperatorInfo(
    val name: String = "",
    val mobile: String = "",
    val userId: String = ""
)

data class ExtractedFamilyCardData(
    val programInfo: ProgramOverviewInfo = ProgramOverviewInfo(),
    val applicant: ApplicantInfo = ApplicantInfo(),
    val permanentAddressDetails: AddressDetails = AddressDetails(),
    val collectionAddressDetails: AddressDetails = AddressDetails(),
    val bankMfsInfo: BankMfsInfo = BankMfsInfo(),
    val householdInfo: HouseholdSocioEconomicInfo = HouseholdSocioEconomicInfo(),
    val additionalAllowanceInfo: AdditionalAllowanceInfo = AdditionalAllowanceInfo(),
    val familyMembers: List<FamilyMemberInfo> = emptyList(),
    val fundUsageItems: List<FundUsageItem> = emptyList(),
    val totalFundUsageTaka: String = "",
    val entryOperatorInfo: EntryOperatorInfo = EntryOperatorInfo(),
    val rawFullText: String = "",
    val pageCount: Int = 1
) {
    private fun esc(value: String): String = buildString(value.length + 2) {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    fun toFormattedJsonString(): String = buildString {
        appendLine("{")
        appendLine("  \"application_header\": {")
        appendLine("    \"application_id\": ${esc(applicant.applicationId)},")
        appendLine("    \"application_date\": ${esc(applicant.applicationDate)},")
        appendLine("    \"main_program\": ${esc(programInfo.mainProgram)},")
        appendLine("    \"sub_program\": ${esc(programInfo.subProgram)},")
        appendLine("    \"family_member_count\": ${esc(programInfo.familyMemberCount)}")
        appendLine("  },")
        appendLine("  \"applicant_personal_info\": {")
        appendLine("    \"name_bn\": ${esc(applicant.nameBangla)},")
        appendLine("    \"name_en\": ${esc(applicant.nameEnglish)},")
        appendLine("    \"gender\": ${esc(applicant.gender)},")
        appendLine("    \"nid\": ${esc(applicant.nid)},")
        appendLine("    \"nationality\": ${esc(applicant.nationality)},")
        appendLine("    \"mobile\": ${esc(applicant.mobile)},")
        appendLine("    \"email\": ${esc(applicant.email)},")
        appendLine("    \"dob\": ${esc(applicant.dateOfBirth)},")
        appendLine("    \"age\": ${esc(applicant.age)},")
        appendLine("    \"father_name_bn\": ${esc(applicant.fatherNameBangla)},")
        appendLine("    \"father_name_en\": ${esc(applicant.fatherNameEnglish)},")
        appendLine("    \"mother_name_bn\": ${esc(applicant.motherNameBangla)},")
        appendLine("    \"mother_name_en\": ${esc(applicant.motherNameEnglish)},")
        appendLine("    \"marital_status\": ${esc(applicant.maritalStatus)},")
        appendLine("    \"religion\": ${esc(applicant.religion)},")
        appendLine("    \"permanent_address\": ${esc(applicant.permanentAddress)},")
        appendLine("    \"collection_address\": ${esc(applicant.presentAddress)}")
        appendLine("  },")
        appendLine("  \"bank_or_mfs_info\": {")
        appendLine("    \"account_type\": ${esc(bankMfsInfo.accountType)},")
        appendLine("    \"bank_name\": ${esc(bankMfsInfo.bankOrMfsName)},")
        appendLine("    \"branch_name\": ${esc(bankMfsInfo.branchName)},")
        appendLine("    \"account_name\": ${esc(bankMfsInfo.accountHolderName)},")
        appendLine("    \"account_number\": ${esc(bankMfsInfo.accountNumber)},")
        appendLine("    \"account_owner\": ${esc(bankMfsInfo.accountOwnerRelation)}")
        appendLine("  },")
        val assetsJson = householdInfo.detectedAssets.joinToString(", ") { esc(it) }
        appendLine("  \"pmt_socio_economic_info\": {")
        appendLine("    \"household_member_count\": ${esc(householdInfo.householdMemberCount)},")
        appendLine("    \"child_count\": ${esc(householdInfo.childCount)},")
        appendLine("    \"head_age\": ${esc(householdInfo.householdHeadAgeRange)},")
        appendLine("    \"head_education\": ${esc(householdInfo.householdHeadEducation)},")
        appendLine("    \"spouse_education\": ${esc(householdInfo.spouseEducation)},")
        appendLine("    \"agri_or_non_agri_laborer\": ${esc(householdInfo.agriOrNonAgriDayLaborerStatus)},")
        appendLine("    \"total_land_ownership\": ${esc(householdInfo.landOwnershipDecimal)},")
        appendLine("    \"received_remittance_last_2_years\": ${esc(householdInfo.receivedRemittanceLast2Years)},")
        appendLine("    \"main_wall_material\": ${esc(householdInfo.mainWallMaterial)},")
        appendLine("    \"separate_dining_room\": ${esc(householdInfo.hasSeparateDiningRoom)},")
        appendLine("    \"separate_kitchen\": ${esc(householdInfo.hasSeparateKitchen)},")
        appendLine("    \"drinking_water_source\": ${esc(householdInfo.drinkingWaterSource)},")
        appendLine("    \"toilet_type\": ${esc(householdInfo.toiletType)},")
        appendLine("    \"owned_assets_raw\": ${esc(householdInfo.ownedAssetsRaw)},")
        appendLine("    \"detected_assets\": [$assetsJson],")
        appendLine("    \"main_roof_material\": ${esc(householdInfo.mainRoofMaterial)},")
        appendLine("    \"total_rooms_count\": ${esc(householdInfo.totalRoomsCount)},")
        appendLine("    \"residence_location_type\": ${esc(householdInfo.residenceAreaType)},")
        appendLine("    \"main_floor_material\": ${esc(householdInfo.mainFloorMaterial)},")
        appendLine("    \"cooking_fuel\": ${esc(householdInfo.mainCookingFuel)},")
        appendLine("    \"electricity_source\": ${esc(householdInfo.electricitySource)},")
        appendLine("    \"land_or_house_ownership_type\": ${esc(householdInfo.landOrHouseOwnershipType)},")
        appendLine("    \"is_ethnic_minority\": ${esc(householdInfo.isEthnicMinority)},")
        appendLine("    \"monthly_lpg_usage_kg\": ${esc(householdInfo.monthlyLpgUsageKg)},")
        appendLine("    \"monthly_diesel_usage_kg\": ${esc(householdInfo.monthlyDieselUsageKg)}")
        appendLine("  },")
        appendLine("  \"additional_allowance_info\": {")
        appendLine("    \"male_members\": ${esc(additionalAllowanceInfo.maleMemberCount)},")
        appendLine("    \"female_members\": ${esc(additionalAllowanceInfo.femaleMemberCount)},")
        appendLine("    \"hijra_members\": ${esc(additionalAllowanceInfo.hijraMemberCount)},")
        appendLine("    \"dss_training_or_benefit\": ${esc(additionalAllowanceInfo.receivedDssTrainingOrBenefit)},")
        appendLine("    \"mobile_banking_account\": ${esc(additionalAllowanceInfo.mobileBankingAccountType)},")
        appendLine("    \"abroad_male_members\": ${esc(additionalAllowanceInfo.abroadMaleCount)},")
        appendLine("    \"abroad_female_members\": ${esc(additionalAllowanceInfo.abroadFemaleCount)},")
        appendLine("    \"abroad_hijra_members\": ${esc(additionalAllowanceInfo.abroadHijraCount)}")
        appendLine("  },")
        appendLine("  \"family_members\": [")
        familyMembers.forEachIndexed { index, member ->
            appendLine("    {")
            appendLine("      \"serial\": ${member.serial},")
            appendLine("      \"name_bn\": ${esc(member.nameBangla)},")
            appendLine("      \"name_en\": ${esc(member.nameEnglish)},")
            appendLine("      \"verification_type\": ${esc(member.verificationType)},")
            appendLine("      \"nid_or_brn\": ${esc(member.nidOrBirthReg)},")
            appendLine("      \"dob\": ${esc(member.dateOfBirthOrAge)},")
            appendLine("      \"relation\": ${esc(member.relationWithApplicant)},")
            appendLine("      \"father_name_bn\": ${esc(member.fatherNameBangla)},")
            appendLine("      \"mother_name_bn\": ${esc(member.motherNameBangla)},")
            appendLine("      \"gender\": ${esc(member.gender)},")
            appendLine("      \"marital_status\": ${esc(member.maritalStatus)},")
            appendLine("      \"religion\": ${esc(member.religion)},")
            appendLine("      \"mobile\": ${esc(member.mobile)},")
            appendLine("      \"profession\": ${esc(member.profession)},")
            appendLine("      \"annual_income\": ${esc(member.estimatedAnnualIncome)},")
            appendLine("      \"livelihood_profession\": ${esc(member.livelihoodProfession)},")
            appendLine("      \"education\": ${esc(member.highestEducation)},")
            appendLine("      \"health_condition\": ${esc(member.healthCondition)}")
            val comma = if (index < familyMembers.lastIndex) "," else ""
            appendLine("    }$comma")
        }
        appendLine("  ],")
        appendLine("  \"entry_operator_info\": {")
        appendLine("    \"name\": ${esc(entryOperatorInfo.name)},")
        appendLine("    \"mobile\": ${esc(entryOperatorInfo.mobile)},")
        appendLine("    \"user_id\": ${esc(entryOperatorInfo.userId)}")
        appendLine("  },")
        appendLine("  \"total_fund_usage_taka\": ${esc(totalFundUsageTaka)},")
        appendLine("  \"page_count\": $pageCount")
        append("}")
    }
}

enum class CriterionCheckStatus {
    PASS,
    WARNING
}

data class CriterionCheckResult(
    val criterionNumber: Int,
    val titleBangla: String,
    val status: CriterionCheckStatus,
    val summaryBangla: String,
    val detailsBangla: List<String> = emptyList(),
    val comparedFields: List<Pair<String, String>> = emptyList()
)

data class FamilyCardVerificationReport(
    val extractedData: ExtractedFamilyCardData,
    val criteriaResults: List<CriterionCheckResult>,
    val formattedJson: String,
    val engineUsedLabel: String,
    val usedOcrFallback: Boolean,
    val extractedCharCount: Int
) {
    val warningCount: Int
        get() = criteriaResults.count { it.status == CriterionCheckStatus.WARNING }

    val passCount: Int
        get() = criteriaResults.count { it.status == CriterionCheckStatus.PASS }

    val isAllConsistent: Boolean
        get() = warningCount == 0
}

enum class QueueItemStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class PdfQueueItem(
    val id: String,
    val uri: Uri?,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val status: QueueItemStatus = QueueItemStatus.PENDING,
    val progressMessage: String = "",
    val report: FamilyCardVerificationReport? = null,
    val pageBitmaps: List<Bitmap> = emptyList(),
    val errorMessage: String? = null
)

enum class QueueFilterMode(val labelBangla: String) {
    ALL("সব ফাইল"),
    WITH_WARNINGS("অসঙ্গতি যুক্ত"),
    CONSISTENT("সামঞ্জস্যপূর্ণ"),
    FAILED("ব্যর্থ")
}
