package com.example.domain

import com.example.data.OcrFamilyCardParser
import com.example.data.PipelineExtractionOutcome
import com.example.model.CriterionCheckResult
import com.example.model.CriterionCheckStatus
import com.example.model.ExtractedFamilyCardData
import com.example.model.FamilyCardVerificationReport

/**
 * Cross-Field Validator for DSS Family Card PDF Applications (পরিবার কার্ড আবেদনপত্র যাচাইকারী).
 */
object FamilyCardValidator {

    fun buildVerificationReport(outcome: PipelineExtractionOutcome): FamilyCardVerificationReport {
        val data = outcome.extractedData
        val criteria = validateAllCriteria(data)
        return FamilyCardVerificationReport(
            extractedData = data,
            criteriaResults = criteria,
            formattedJson = data.toFormattedJsonString(),
            engineUsedLabel = outcome.engineUsedLabel,
            usedOcrFallback = outcome.usedOcrFallback,
            extractedCharCount = outcome.extractedCharCount
        )
    }

    fun validateAllFiveCriteria(data: ExtractedFamilyCardData): List<CriterionCheckResult> =
        validateAllCriteria(data)

    fun validateAllCriteria(data: ExtractedFamilyCardData): List<CriterionCheckResult> {
        return listOf(
            checkCriterion1OcrAndJsonCompleteness(data),
            checkCriterion2MaritalStatusConsistency(data),
            checkCriterion3ExpatriateVsLivelihood(data),
            checkCriterion4TotalLandOwnership(data),
            checkCriterion5ToiletTypePitCheck(data),
            checkCriterion6DesignatedAssets(data)
        )
    }

    /**
     * Criterion 1: টেক্সট/OCR এক্সট্রাকশন ও JSON পূর্ণতা যাচাই
     */
    private fun checkCriterion1OcrAndJsonCompleteness(data: ExtractedFamilyCardData): CriterionCheckResult {
        val missingFields = mutableListOf<String>()
        if (data.applicant.nameBangla.isBlank() && data.applicant.nameEnglish.isBlank()) {
            missingFields.add("আবেদনকারীর নাম")
        }
        if (data.applicant.nid.isBlank()) {
            missingFields.add("এনআইডি নম্বর")
        }
        if (data.applicant.maritalStatus.isBlank()) {
            missingFields.add("বৈবাহিক অবস্থা (ব্যক্তিগত তথ্য)")
        }
        if (data.familyMembers.isEmpty()) {
            missingFields.add("পরিবারের সদস্য তালিকা")
        }
        if (data.householdInfo.landOwnershipDecimal.isBlank() && data.householdInfo.toiletType.isBlank()) {
            missingFields.add("পিএমটি (PMT) আর্থ-সামাজিক তথ্য")
        }

        val isComplete = missingFields.isEmpty()
        val summary = if (isComplete) {
            "আবেদনপত্রের সকল আবশ্যক সেকশন (ব্যক্তিগত তথ্য, ঠিকানা, ব্যাংক, পিএমটি তথ্য ও ${data.familyMembers.size} জন সদস্যের তথ্য) সফলভাবে এক্সট্রাক্ট ও JSON-এ রূপান্তরিত হয়েছে।"
        } else {
            "আবেদনপত্রে ${missingFields.size}টি আবশ্যক তথ্য অনুপস্থিত বা অস্পষ্ট: ${missingFields.joinToString(", ")}।"
        }

        return CriterionCheckResult(
            criterionNumber = 1,
            titleBangla = "১. টেক্সট/OCR এক্সট্রাকশন ও JSON পূর্ণতা",
            status = if (isComplete) CriterionCheckStatus.PASS else CriterionCheckStatus.WARNING,
            summaryBangla = summary,
            detailsBangla = if (isComplete) {
                listOf(
                    "আবেদন আইডি: ${data.applicant.applicationId.ifBlank { "শনাক্তকৃত" }}, আবেদনকারী: ${data.applicant.nameBangla}",
                    "পরিবারের সদস্য সংখ্যা: ${data.familyMembers.size} জন, পৃষ্ঠা সংখ্যা: ${data.pageCount}"
                )
            } else {
                missingFields.map { "অনুপস্থিত বা অস্পষ্ট ফিল্ড: $it" }
            },
            comparedFields = listOf(
                "আবেদন আইডি" to data.applicant.applicationId.ifBlank { "পাওয়া যায়নি" },
                "আবেদনকারীর নাম" to data.applicant.nameBangla.ifBlank { data.applicant.nameEnglish.ifBlank { "পাওয়া যায়নি" } },
                "এনআইডি নম্বর" to data.applicant.nid.ifBlank { "পাওয়া যায়নি" },
                "শনাক্তকৃত সদস্য সংখ্যা" to "${data.familyMembers.size} জন"
            )
        )
    }

    /**
     * Criterion 2: বৈবাহিক অবস্থা যাচাই (ব্যক্তিগত তথ্য বনাম পরিবারের সদস্য ১)
     */
    private fun checkCriterion2MaritalStatusConsistency(data: ExtractedFamilyCardData): CriterionCheckResult {
        val personalStatus = normalizeMaritalStatus(data.applicant.maritalStatus)
        val member1 = data.familyMembers.firstOrNull { it.serial == 1 } ?: data.familyMembers.firstOrNull()
        val member1Status = normalizeMaritalStatus(member1?.maritalStatus.orEmpty())

        val status: CriterionCheckStatus
        val summary: String
        val details = mutableListOf<String>()

        when {
            personalStatus.isBlank() || member1Status.isBlank() -> {
                status = CriterionCheckStatus.WARNING
                summary = "ব্যক্তিগত তথ্য অথবা পরিবারের সদস্য ১-এর বৈবাহিক অবস্থা খালি থাকায় তুলনা করা সম্ভব হয়নি।"
                details.add("ব্যক্তিগত তথ্যে বৈবাহিক অবস্থা: '${data.applicant.maritalStatus.ifBlank { "অপ্রাপ্য" }}'")
                details.add("সদস্য ১-এর বৈবাহিক অবস্থা: '${member1?.maritalStatus?.ifBlank { "অপ্রাপ্য" } ?: "সদস্য ১ পাওয়া যায়নি"}'")
            }
            personalStatus == member1Status -> {
                status = CriterionCheckStatus.PASS
                summary = "ব্যক্তিগত তথ্যের বৈবাহিক অবস্থা ('$personalStatus') এবং পরিবারের সদস্য ১-এর বৈবাহিক অবস্থা ('$member1Status') সম্পূর্ণ সামঞ্জস্যপূর্ণ।"
                details.add("উভয় স্থানেই বৈবাহিক অবস্থা '$personalStatus' হিসেবে মিল পাওয়া গেছে।")
            }
            else -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! ব্যক্তিগত তথ্যে বৈবাহিক অবস্থা '$personalStatus' কিন্তু পরিবারের সদস্য ১-এর তথ্যে '$member1Status' উল্লেখ করা হয়েছে।"
                details.add("আবেদনকারীর ব্যক্তিগত তথ্য এবং সদস্য ১-এর বৈবাহিক অবস্থা অভিন্ন হওয়া আবশ্যক।")
            }
        }

        return CriterionCheckResult(
            criterionNumber = 2,
            titleBangla = "২. বৈবাহিক অবস্থা যাচাই (ব্যক্তিগত বনাম সদস্য ১)",
            status = status,
            summaryBangla = summary,
            detailsBangla = details,
            comparedFields = listOf(
                "ব্যক্তিগত তথ্যে বৈবাহিক অবস্থা" to data.applicant.maritalStatus.ifBlank { "পাওয়া যায়নি" },
                "সদস্য ১-এর বৈবাহিক অবস্থা" to (member1?.maritalStatus?.ifBlank { "পাওয়া যায়নি" } ?: "সদস্য ১ নেই")
            )
        )
    }

    /**
     * Criterion 3: প্রবাসী সদস্য বনাম জীবিকার পেশা ('অন্যান্য') যাচাই
     */
    private fun checkCriterion3ExpatriateVsLivelihood(data: ExtractedFamilyCardData): CriterionCheckResult {
        val abroadCountFromAllowance = data.additionalAllowanceInfo.totalAbroadCount
        val receivedForeignRemittance = data.householdInfo.receivedRemittanceLast2Years.contains("হ্যাঁ")
        val expatMembersInList = data.familyMembers.filter {
            it.isExpatriateOrAbroad ||
                it.profession.contains("প্রবাস") ||
                it.livelihoodProfession.contains("প্রবাস")
        }
        val legacyExpatFlag = data.householdInfo.hasExpatriateMemberField.contains("হ্যাঁ") ||
            data.householdInfo.hasExpatriateMemberField.contains("আছে")

        val hasAnyExpat = abroadCountFromAllowance > 0 ||
            receivedForeignRemittance ||
            expatMembersInList.isNotEmpty() ||
            legacyExpatFlag

        val memberLivelihoods = data.familyMembers
            .map { it.livelihoodProfession.ifBlank { it.profession }.trim() }
            .filter { it.isNotBlank() }
        val primaryLivelihood = data.householdInfo.primaryLivelihoodProfession.trim()
        val allLivelihoodsCombined = (memberLivelihoods + listOfNotNull(primaryLivelihood.takeIf { it.isNotBlank() }))
            .distinct()

        val hasOtherLivelihood = allLivelihoodsCombined.any {
            it.contains("অন্যান্য") || it.contains("প্রবাস") || it.contains("রেমিট্যান্স")
        }

        val status: CriterionCheckStatus
        val summary: String
        val details = mutableListOf<String>()

        if (!hasAnyExpat) {
            if (hasOtherLivelihood) {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! বিদেশে বসবাসরত সদস্য সংখ্যা ০ (এবং বৈদেশিক অর্থ গ্রহণ 'না'), কিন্তু জীবিকার পেশা হিসেবে 'অন্যান্য' উল্লেখ করা হয়েছে।"
                details.add("প্রবাসী সদস্য না থাকলে জীবিকার পেশায় সুনির্দিষ্ট পেশা উল্লেখ থাকা প্রয়োজন।")
            } else {
                status = CriterionCheckStatus.PASS
                val shownProf = allLivelihoodsCombined.joinToString(", ").ifBlank { "গৃহিণী / নির্ধারিত" }
                summary = "পরিবারে কোনো প্রবাসী সদস্য নেই এবং সদস্যদের জীবিকার পেশা ($shownProf) সামঞ্জস্যপূর্ণ।"
                details.add("বিদেশে বসবাসরত সদস্য সংখ্যা: ০ জন; গত ২ বছরে বৈদেশিক অর্থ গ্রহণ: ${data.householdInfo.receivedRemittanceLast2Years.ifBlank { "না" }}।")
            }
        } else {
            if (hasOtherLivelihood) {
                status = CriterionCheckStatus.PASS
                summary = "পরিবারে প্রবাসী সদস্য রয়েছে এবং নিয়ম অনুযায়ী জীবিকার পেশা 'অন্যান্য' উল্লেখ করা হয়েছে।"
                details.add("প্রবাসী সদস্য ও জীবিকার পেশা ('অন্যান্য') পরস্পর সামঞ্জস্যপূর্ণ।")
            } else {
                status = CriterionCheckStatus.WARNING
                val shownProf = allLivelihoodsCombined.joinToString(", ").ifBlank { "অপ্রাপ্য" }
                summary = "অসঙ্গতির সতর্কবার্তা! পরিবারে প্রবাসী সদস্য থাকা সত্ত্বেও জীবিকার পেশা 'অন্যান্য' না দিয়ে '$shownProf' দেওয়া হয়েছে।"
                details.add("নির্দেশনা অনুযায়ী পরিবারে প্রবাসী সদস্য থাকলে জীবিকার পেশা 'অন্যান্য' হওয়া আবশ্যক।")
            }
        }

        val abroadSummaryLabel = if (hasAnyExpat) {
            "হ্যাঁ (${abroadCountFromAllowance.coerceAtLeast(expatMembersInList.size).coerceAtLeast(1)} জন)"
        } else {
            "না (০ জন)"
        }

        return CriterionCheckResult(
            criterionNumber = 3,
            titleBangla = "৩. প্রবাসী সদস্য বনাম জীবিকার পেশা ('অন্যান্য')",
            status = status,
            summaryBangla = summary,
            detailsBangla = details,
            comparedFields = listOf(
                "বিদেশে বসবাসরত সদস্য" to abroadSummaryLabel,
                "গত ২ বছরে বৈদেশিক অর্থ গ্রহণ" to data.householdInfo.receivedRemittanceLast2Years.ifBlank { "না" },
                "সদস্যদের জীবিকার পেশা" to allLivelihoodsCombined.joinToString(", ").ifBlank { "পাওয়া যায়নি" }
            )
        )
    }

    /**
     * Criterion 4: খানায় মোট জমির পরিমাণ যাচাই ('খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত?')
     * যদি 'কোন জমি নেই' উল্লেখ থাকে তাহলে তা চিহ্নিত করে অসঙ্গতির সতর্কবার্তা দিবে।
     */
    private fun checkCriterion4TotalLandOwnership(data: ExtractedFamilyCardData): CriterionCheckResult {
        val landRaw = data.householdInfo.landOwnershipDecimal.trim()
        val houseOwnershipType = data.householdInfo.landOrHouseOwnershipType.trim()

        val isNoLandDeclared = landRaw.contains("কোন জমি নেই") ||
            landRaw.contains("কোনো জমি নেই") ||
            landRaw.contains("জমি নেই") ||
            landRaw.contains("জমি নাই") ||
            landRaw.equals("নাই", ignoreCase = true) ||
            landRaw.equals("নেই", ignoreCase = true) ||
            landRaw.equals("০", ignoreCase = true) ||
            landRaw.equals("0", ignoreCase = true) ||
            landRaw.contains("শূন্য")

        val status: CriterionCheckStatus
        val summary: String
        val details = mutableListOf<String>()

        when {
            landRaw.isBlank() -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! 'খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত?' ফিল্ডে কোনো তথ্য পাওয়া যায়নি।"
                details.add("পিএমটি তথ্যে খানার মোট জমির পরিমাণ উল্লেখ থাকা আবশ্যক।")
            }
            isNoLandDeclared -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ হিসেবে '$landRaw' উল্লেখ করা হয়েছে।"
                details.add("চিহ্নিত অসঙ্গতি: 'খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত?' প্রশ্নে '$landRaw' দেওয়া হয়েছে, যা যাচাইযোগ্য।")
                if (houseOwnershipType.isNotBlank()) {
                    details.add("উল্লেখ্য, ভূমির বা বসতঘরের মালিকানা ধরণ হিসেবে '$houseOwnershipType' উল্লেখ রয়েছে।")
                }
            }
            else -> {
                status = CriterionCheckStatus.PASS
                summary = "খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ '$landRaw' উল্লেখ রয়েছে, যা সামঞ্জস্যপূর্ণ।"
                details.add("শনাক্তকৃত মোট জমির পরিমাণ: $landRaw")
                if (houseOwnershipType.isNotBlank()) {
                    details.add("ভূমির বা বসতঘরের মালিকানা ধরণ: $houseOwnershipType")
                }
            }
        }

        return CriterionCheckResult(
            criterionNumber = 4,
            titleBangla = "৪. খানায় মোট জমির পরিমাণ যাচাই",
            status = status,
            summaryBangla = summary,
            detailsBangla = details,
            comparedFields = buildList {
                add("মালিকানাধীন মোট জমির পরিমাণ" to landRaw.ifBlank { "পাওয়া যায়নি" })
                if (houseOwnershipType.isNotBlank()) {
                    add("ভূমির/বসতঘরের মালিকানা ধরণ" to houseOwnershipType)
                }
            }
        )
    }

    /**
     * Criterion 5: পায়খানা/টয়লেটের ধরন ('পিট/গর্ত' পরীক্ষা)
     */
    private fun checkCriterion5ToiletTypePitCheck(data: ExtractedFamilyCardData): CriterionCheckResult {
        val toiletRaw = data.householdInfo.toiletType.trim()
        val hasPitOrHoleKeyword = toiletRaw.contains("পিট") ||
            toiletRaw.contains("গর্ত") ||
            toiletRaw.contains("কাঁচা") ||
            toiletRaw.contains("খোলা") ||
            toiletRaw.contains("নাই") ||
            toiletRaw.contains("pit", ignoreCase = true)

        val status: CriterionCheckStatus
        val summary: String
        val details = mutableListOf<String>()

        when {
            toiletRaw.isBlank() -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! 'খানার সদস্যগণ কি ধরণের পায়খানা/টয়লেট ব্যবহার করে?' ফিল্ডটি খালি রয়েছে।"
            }
            hasPitOrHoleKeyword -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! পায়খানা/টয়লেটের ধরন হিসেবে '$toiletRaw' (পিট/গর্ত/কাঁচা) উল্লেখ করা হয়েছে, যা সরেজমিন যাচাই প্রয়োজন।"
                details.add("টয়লেটের ধরনে 'পিট' বা 'গর্ত' উল্লেখ থাকলে তা অসঙ্গতির সতর্কবার্তা হিসেবে চিহ্নিত করা হয়।")
            }
            else -> {
                status = CriterionCheckStatus.PASS
                summary = "খানার ব্যবহৃত পায়খানা/টয়লেটের ধরন '$toiletRaw' (পিট/গর্ত নয়), যা সামঞ্জস্যপূর্ণ।"
                details.add("শনাক্তকৃত টয়লেটের ধরন: $toiletRaw")
            }
        }

        return CriterionCheckResult(
            criterionNumber = 5,
            titleBangla = "৫. পায়খানা/টয়লেটের ধরন ('পিট/গর্ত' পরীক্ষা)",
            status = status,
            summaryBangla = summary,
            detailsBangla = details,
            comparedFields = listOf(
                "ব্যবহৃত পায়খানা/টয়লেটের ধরন" to toiletRaw.ifBlank { "পাওয়া যায়নি" },
                "পিট/গর্ত পরীক্ষা ফলাফল" to if (hasPitOrHoleKeyword) "পিট/গর্ত শনাক্ত (সতর্কবার্তা)" else "স্বাভাবিক / সামঞ্জস্যপূর্ণ"
            )
        )
    }

    /**
     * Criterion 6: ৬টি নির্ধারিত পরিসম্পদ পরীক্ষা ('খানার মালিকানাধীন নিম্নের পরিসম্পদ গুলোর কোনটি আছে ?')
     */
    private fun checkCriterion6DesignatedAssets(data: ExtractedFamilyCardData): CriterionCheckResult {
        val rawAssets = data.householdInfo.ownedAssetsRaw.trim()
        val detected = data.householdInfo.detectedAssets
        val allSixNames = OcrFamilyCardParser.SIX_DESIGNATED_ASSETS.map { it.first }

        val highValueAssets = detected.filter {
            it.contains("মোটরসাইকেল") || it.contains("কম্পিউটার") || it.contains("এসি") || it.contains("ফ্রিজ") || it.contains("রেফ্রিজারেটর")
        }
        val isNoAssetAtAll = rawAssets.isBlank() ||
            rawAssets.contains("কোনটিই নেই") ||
            rawAssets.equals("নাই", ignoreCase = true) ||
            rawAssets.equals("নেই", ignoreCase = true)

        val status: CriterionCheckStatus
        val summary: String

        when {
            isNoAssetAtAll -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! খানার মালিকানাধীন পরিসম্পদ হিসেবে '${rawAssets.ifBlank { "কিছুই উল্লেখ নেই" }}' দেওয়া হয়েছে।"
            }
            highValueAssets.isNotEmpty() -> {
                status = CriterionCheckStatus.WARNING
                summary = "অসঙ্গতির সতর্কবার্তা! পরিসম্পদ তালিকায় উচ্চমূল্যের সম্পদ (${highValueAssets.joinToString(", ")}) পাওয়া গেছে।"
            }
            else -> {
                status = CriterionCheckStatus.PASS
                summary = "খানার মালিকানাধীন পরিসম্পদ হিসেবে '$rawAssets' উল্লেখ রয়েছে এবং কোনো অসামঞ্জস্যপূর্ণ উচ্চমূল্যের সম্পদ নেই।"
            }
        }

        val details = allSixNames.map { assetName ->
            val present = detected.contains(assetName)
            if (present) "✓ উল্লেখ আছে: $assetName" else "• উল্লেখ নেই: $assetName"
        }

        return CriterionCheckResult(
            criterionNumber = 6,
            titleBangla = "৬. নির্ধারিত পরিসম্পদ পরীক্ষা",
            status = status,
            summaryBangla = summary,
            detailsBangla = details,
            comparedFields = listOf(
                "উল্লেখিত পরিসম্পদসমূহ" to rawAssets.ifBlank { "নাই / উল্লেখ নেই" },
                "শনাক্তকৃত পরিসম্পদ সংখ্যা" to "${detected.size}টি (${detected.joinToString(", ").ifBlank { "নাই" }})"
            )
        )
    }

    private fun normalizeMaritalStatus(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.contains("অবিবাহিত") -> "অবিবাহিত"
            trimmed.contains("বিবাহিত") -> "বিবাহিত"
            trimmed.contains("বিধবা") -> "বিধবা"
            trimmed.contains("বিপত্নীক") -> "বিপত্নীক"
            trimmed.contains("তালাক") -> "তালাকপ্রাপ্ত"
            trimmed.contains("পরিত্যক্ত") -> "স্বামী পরিত্যক্তা"
            else -> trimmed
        }
    }
}
