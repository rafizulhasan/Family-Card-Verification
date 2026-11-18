package com.example.data

import com.example.model.AdditionalAllowanceInfo
import com.example.model.AddressDetails
import com.example.model.ApplicantInfo
import com.example.model.BankMfsInfo
import com.example.model.EntryOperatorInfo
import com.example.model.ExtractedFamilyCardData
import com.example.model.FamilyMemberInfo
import com.example.model.FundUsageItem
import com.example.model.HouseholdSocioEconomicInfo
import com.example.model.ProgramOverviewInfo

object OcrFamilyCardParser {

    val SIX_DESIGNATED_ASSETS: List<Pair<String, List<String>>> = listOf(
        "মোবাইল ফোন" to listOf("মোবাইল ফোন", "মোবাইল", "mobile phone"),
        "বৈদ্যুতিক পাখা" to listOf("বৈদ্যুতিক পাখা", "পাখা", "ফ্যান", "electric fan"),
        "খাট / চৌকি" to listOf("খাট", "চৌকি", "পালং"),
        "টেলিভিশন / টিভি" to listOf("টেলিভিশন", "টিভি", "রঙিন টিভি", "স্মার্ট টিভি", "television", "tv"),
        "রেফ্রিজারেটর / ফ্রিজ" to listOf("ফ্রিজ", "রেফ্রিজারেটর", "ডিপ ফ্রিজ", "refrigerator", "fridge"),
        "মোটরসাইকেল / কম্পিউটার / এসি" to listOf(
            "মোটরসাইকেল", "মোটর সাইকেল", "মোটরগাড়ি", "মোটরগাড়ি",
            "কম্পিউটার", "ল্যাপটপ", "এসি", "এয়ার কন্ডিশনার", "এয়ার কন্ডিশনার"
        )
    )

    fun parseFromOcrPages(pagesText: List<String>): ExtractedFamilyCardData {
        val combined = pagesText.joinToString("\n\n")
        return parseFromOcrText(combined).copy(pageCount = pagesText.size.coerceAtLeast(1))
    }

    fun parseFromOcrText(rawText: String): ExtractedFamilyCardData {
        val rawLines = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { line ->
                val cleaned = line.replace(Regex("[\\t ]+"), " ").trim()
                if (cleaned.any { it.code in 0xE000..0xE1FF }) {
                    BengaliPdfUnicodeDecoder.decodeVisualBengali(cleaned)
                } else {
                    cleaned
                }
            }
            .filter { it.isNotBlank() }

        val normalizedLines = mergeBrokenMultilineQuestions(rawLines)
        val fullLinesText = normalizedLines.joinToString("\n")

        // Section-sliced line lists to avoid label collisions between Personal Info, Address, and Member blocks
        val personalInfoLines = sliceSectionLines(
            normalizedLines,
            startMarkers = listOf("ব্যক্তিগত তথ্য"),
            endMarkers = listOf("স্থায়ী ঠিকানা", "স্থায়ী ঠিকানা", "ব্যাংক / এমএফএস তথ্য", "পিএমটি তথ্য")
        ).ifEmpty {
            normalizedLines.takeWhile { !it.startsWith("পরিবারের সদস্য") && !it.startsWith("সদস্য ১") }
        }

        val addressAndBankLines = sliceSectionLines(
            normalizedLines,
            startMarkers = listOf("স্থায়ী ঠিকানা", "স্থায়ী ঠিকানা"),
            endMarkers = listOf("পিএমটি তথ্য", "পরিবারের সদস্য")
        )

        val pmtAndAllowanceLines = sliceSectionLines(
            normalizedLines,
            startMarkers = listOf("পিএমটি তথ্য"),
            endMarkers = listOf("পরিবারের সদস্য", "সদস্য ১")
        ).ifEmpty { normalizedLines }

        val entryOperatorLines = sliceSectionLines(
            normalizedLines,
            startMarkers = listOf("এন্ট্রি ব্যবহারকারীর তথ্য"),
            endMarkers = emptyList()
        )

        val fundUsageLines = sliceSectionLines(
            normalizedLines,
            startMarkers = listOf("অর্থ ব্যবহার"),
            endMarkers = listOf("এন্ট্রি ব্যবহারকারীর তথ্য")
        )

        // 1. Header & Program Overview
        val applicationId = extractValueByLabels(
            normalizedLines,
            listOf("আবেদন আইডি", "আবেদন নং", "ট্র্যাকিং নম্বর", "Application ID")
        )
        val applicationDate = extractValueByLabels(
            normalizedLines,
            listOf("আবেদনের তারিখ", "Application Date")
        )
        val mainProgram = extractValueByLabels(normalizedLines, listOf("প্রধান কার্যক্রম"))
        val subProgram = extractValueByLabels(normalizedLines, listOf("উপ-কার্যক্রম"))
        val familyMemberCountHeader = extractValueByLabels(normalizedLines, listOf("পরিবারের সদস্য সংখ্যা"))

        // 2. Applicant Personal Info
        val nameEnglish = extractValueByLabels(
            personalInfoLines,
            listOf("নাম (ইংরেজি)", "আবেদনকারীর নাম (ইংরেজিতে)", "Applicant Name (English)")
        )
        val nameBangla = extractValueByLabels(
            personalInfoLines,
            listOf("নাম (বাংলা)", "আবেদনকারীর নাম (বাংলায়)", "আবেদনকারীর নাম (বাংলায়)", "আবেদনকারীর নাম")
        )
        val gender = extractValueByLabels(personalInfoLines, listOf("লিঙ্গ"))
        val nid = extractValueByLabels(
            personalInfoLines,
            listOf("এনআইডি নম্বর", "জাতীয় পরিচয়পত্র নম্বর", "জাতীয় পরিচয়পত্র নম্বর")
        ).ifBlank {
            Regex("\\b([০-৯0-9]{10}|[০-৯0-9]{13}|[০-৯0-9]{17})\\b").find(personalInfoLines.joinToString("\n"))?.value.orEmpty()
        }
        val nationality = extractValueByLabels(personalInfoLines, listOf("জাতীয়তা", "জাতীয়তা"))
        val mobile = extractValueByLabels(
            personalInfoLines,
            listOf("মোবাইল নম্বর", "মোবাইল")
        )
        val email = extractValueByLabels(personalInfoLines, listOf("ইমেইল"))
        val dob = extractValueByLabels(personalInfoLines, listOf("জন্ম তারিখ"))
        val age = extractValueByLabels(personalInfoLines, listOf("বয়স", "বয়স"))
        val fatherNameEnglish = extractValueByLabels(personalInfoLines, listOf("পিতার নাম (ইংরেজি)"))
        val fatherNameBangla = extractValueByLabels(
            personalInfoLines,
            listOf("পিতার নাম (বাংলা)", "পিতার নাম")
        )
        val motherNameEnglish = extractValueByLabels(personalInfoLines, listOf("মাতার নাম (ইংরেজি)"))
        val motherNameBangla = extractValueByLabels(
            personalInfoLines,
            listOf("মাতার নাম (বাংলা)", "মাতার নাম")
        )
        val spouseName = extractValueByLabels(
            personalInfoLines,
            listOf("স্বামী/স্ত্রীর নাম", "স্বামীর নাম", "স্ত্রীর নাম")
        )
        val maritalStatus = extractPersonalMaritalStatus(personalInfoLines)
        val religion = extractValueByLabels(personalInfoLines, listOf("ধর্ম"))
        val applicantProfession = extractValueByLabels(
            personalInfoLines,
            listOf("আবেদনকারীর পেশা", "ব্যক্তিগত পেশা", "পেশা")
        )

        // 3. Addresses & Bank Info
        val (permAddress, collAddress) = extractDualAddresses(addressAndBankLines)
        val permAddressStr = permAddress.toFormattedLine().ifBlank {
            extractValueByLabels(normalizedLines, listOf("স্থায়ী ঠিকানা", "স্থায়ী ঠিকানা"))
        }
        val collAddressStr = collAddress.toFormattedLine().ifBlank {
            extractValueByLabels(normalizedLines, listOf("বর্তমান ঠিকানা", "তথ্য সংগ্রহের ঠিকানা"))
        }

        val bankInfo = BankMfsInfo(
            accountType = extractValueByLabels(addressAndBankLines, listOf("হিসাবের ধরন")),
            bankOrMfsName = extractValueByLabels(addressAndBankLines, listOf("ব্যাংক")),
            branchName = extractValueByLabels(addressAndBankLines, listOf("শাখা")),
            accountHolderName = extractValueByLabels(addressAndBankLines, listOf("হিসাবের নাম")),
            accountNumber = extractValueByLabels(addressAndBankLines, listOf("হিসাব নম্বর")),
            accountOwnerRelation = extractValueByLabels(addressAndBankLines, listOf("হিসাবের মালিক"))
        )

        // 4. PMT Socio-Economic Info (24 fields)
        val hhMemberCount = extractValueByLabels(pmtAndAllowanceLines, listOf("খানার সদস্য সংখ্যা"))
        val hhChildCount = extractValueByLabels(pmtAndAllowanceLines, listOf("খানার শিশু সংখ্যা"))
        val hhHeadAge = extractValueByLabels(pmtAndAllowanceLines, listOf("খানার প্রধানের বয়স", "খানার প্রধানের বয়স"))
        val hhHeadEdu = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানা প্রধানের শিক্ষা (সর্বোচ্চ ডিগ্রি সম্পন্ন)", "খানা প্রধানের শিক্ষা")
        )
        val spouseEdu = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("স্বামীর শিক্ষা (সর্বোচ্চ ডিগ্রি সম্পন্ন)", "স্বামীর শিক্ষা")
        )
        val agriLaborStatus = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "খানার কোন সদস্য কি কৃষি অথবা অকৃষি শ্রমিক হিসেবে নিয়োজিত আছেন?",
                "খানার কোন সদস্য কি কৃষি অথবা অকৃষি শ্রমিক হিসেবে নিয়োজিত আছেন?",
                "কৃষি অথবা অকৃষি শ্রমিক হিসেবে নিয়োজিত আছেন?",
                "কৃষি অথবা অকৃষি শ্রমিক"
            )
        )
        val totalLandOwnership = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত?",
                "খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত",
                "মালিকানাধীন মোট জমির পরিমাণ কত?",
                "মালিকানাধীন মোট জমির পরিমাণ",
                "মোট জমির পরিমাণ",
                "জমির পরিমাণ (শতাংশ)",
                "জমির পরিমাণ"
            )
        )
        val receivedRemittance2Yrs = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "গত ২ বছরে আপনার খানা বিদেশে অবস্থানরত কারো নিকট হতে কোন অর্থ গ্রহণ করেছে কি?",
                "বিদেশে অবস্থানরত কারো নিকট হতে কোন অর্থ গ্রহণ করেছে কি?",
                "বিদেশে অবস্থানরত কারো নিকট হতে কোন অর্থ গ্রহণ"
            )
        )
        val wallMaterial = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানার প্রধান ঘরের দেয়াল কি দিয়ে তৈরী?", "খানার প্রধান ঘরের দেয়াল কি দিয়ে তৈরী?", "ঘরের দেয়াল কি দিয়ে তৈরী")
        )
        val separateDining = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানায় কি পৃথক খাবার ঘর/কক্ষ আছে?", "খানায় কি পৃথক খাবার ঘর/কক্ষ আছে?")
        )
        val separateKitchen = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানায় কি পৃথক রান্নাঘর আছে?", "খানায় কি পৃথক রান্নাঘর আছে?")
        )
        val waterSource = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানায় খাবার পানির উৎস কি?", "খানায় খাবার পানির উৎস কি?", "খাবার পানির উৎস")
        )
        val toiletType = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "খানার সদস্যগণ কি ধরণের পায়খানা/টয়লেট ব্যবহার করে?",
                "খানার সদস্যগণ কি ধরণের পায়খানা/টয়লেট ব্যবহার করে?",
                "কি ধরণের পায়খানা/টয়লেট ব্যবহার করে?",
                "কি ধরণের পায়খানা/টয়লেট ব্যবহার করে?",
                "টয়লেটের ধরন",
                "টয়লেটের ধরন",
                "পায়খানার ধরন"
            )
        )
        val ownedAssetsRaw = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "খানার মালিকানাধীন নিম্নের পরিসম্পদ গুলোর কোনটি আছে ? (একাধিক উত্তর হতে পারে)",
                "খানার মালিকানাধীন নিম্নের পরিসম্পদ গুলোর কোনটি আছে ?",
                "নিম্নের পরিসম্পদ গুলোর কোনটি আছে",
                "পরিবারের পরিসম্পদ",
                "পরিসম্পদের বিবরণ",
                "পরিসম্পদ"
            )
        )
        val detectedAssets = detectSixAssets(ownedAssetsRaw, pmtAndAllowanceLines.joinToString("\n"))
        val roofMaterial = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানার প্রধান ঘরের ছাদ কি দিয়ে তৈরী", "খানার প্রধান ঘরের ছাদ কি দিয়ে তৈরী")
        )
        val totalRooms = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানায় মোট রুম/কক্ষ সংখ্যা", "খানায় মোট রুম/কক্ষ সংখ্যা")
        )
        val residenceLocation = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানাটি বা বাসস্থানটি কোথায় অবস্থিত", "খানাটি বা বাসস্থানটি কোথায় অবস্থিত")
        )
        val floorMaterial = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানার প্রধান বসতঘরের মেঝে বা ফ্লোর কি দিয়ে তৈরী", "খানার প্রধান বসতঘরের মেঝে বা ফ্লোর কি দিয়ে তৈরী", "মেঝে বা ফ্লোর কি দিয়ে তৈরী")
        )
        val cookingFuel = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("রান্নার জ্বালানির প্রধান উপকরণ কি")
        )
        val electricitySource = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানায় বিদ্যুৎ লাইনের উৎস", "খানায় বিদ্যুৎ লাইনের উৎস")
        )
        val landOwnershipType = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("ভূমির বা বসতঘরের মালিকানা ধরণ", "ভূমির বা বসতঘরের মালিকানা ধরন")
        )
        val ethnicMinority = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("খানাটি ক্ষুদ্র নৃগোষ্ঠী কি না")
        )
        val lpgUsage = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "খানায় রান্নার জ্বালানির প্রধান উপকরণ এলপিজি গ্যাস হলে প্রতিমাসে এলপিজি গ্যাস ব্যবহারের পরিমান",
                "খানায় রান্নার জ্বালানির প্রধান উপকরণ এলপিজি গ্যাস হলে প্রতিমাসে এলপিজি গ্যাস ব্যবহারের পরিমান",
                "প্রতিমাসে এলপিজি গ্যাস ব্যবহারের পরিমান"
            )
        )
        val dieselUsage = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf(
                "কৃষি কাজে ডিজেল ব্যবহার হলে তার মাসিক আনুমানিক পরিমাণ",
                "ডিজেল ব্যবহার হলে তার মাসিক আনুমানিক পরিমাণ"
            )
        )

        // 5. Additional Allowance Info
        val allowanceInfo = AdditionalAllowanceInfo(
            maleMemberCount = extractValueByLabels(pmtAndAllowanceLines, listOf("পরিবারে পুরুষ সদস্য সংখ্যা")),
            femaleMemberCount = extractValueByLabels(pmtAndAllowanceLines, listOf("পরিবারে নারী সদস্য সংখ্যা")),
            hijraMemberCount = extractValueByLabels(pmtAndAllowanceLines, listOf("পরিবারে হিজড়া সদস্য সংখ্যা", "পরিবারে হিজড়া সদস্য সংখ্যা")),
            receivedDssTrainingOrBenefit = extractValueByLabels(
                pmtAndAllowanceLines,
                listOf("সমাজসেবা অধিদপ্তর থেকে কোন প্রশিক্ষণ বা আর্থিক সুবিধা গ্রহণ করেছেন")
            ),
            mobileBankingAccountType = extractValueByLabels(
                pmtAndAllowanceLines,
                listOf(
                    "খানা প্রধানের প্রদত্ত মোবাইল নাম্বারটি নিম্নবর্ণিত মোবাইল ব্যাংকিং-এ কোন হিসাব নম্বর আছে কি?",
                    "মোবাইল ব্যাংকিং-এ কোন হিসাব নম্বর আছে কি?"
                )
            ),
            abroadMaleCount = extractValueByLabels(pmtAndAllowanceLines, listOf("বিদেশে বসবাসরত পরিবারের পুরুষ সদস্য সংখ্যা")),
            abroadFemaleCount = extractValueByLabels(pmtAndAllowanceLines, listOf("বিদেশে বসবাসরত পরিবারের মহিলা সদস্য সংখ্যা")),
            abroadHijraCount = extractValueByLabels(pmtAndAllowanceLines, listOf("বিদেশে বসবাসরত পরিবারের হিজড়া সদস্য সংখ্যা", "বিদেশে বসবাসরত পরিবারের হিজড়া সদস্য সংখ্যা"))
        )

        // 6. Family Members (supports both multi-row DSS member blocks "সদস্য ১ — ..." and single-row tables)
        val familyMembers = extractFamilyMembers(normalizedLines, nameBangla)

        val primaryLivelihood = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("পরিবারের প্রধান জীবিকার পেশা", "জীবিকার প্রধান উৎস", "জীবিকার পেশা")
        ).ifBlank {
            familyMembers.firstOrNull()?.livelihoodProfession?.ifBlank {
                familyMembers.firstOrNull()?.profession.orEmpty()
            }.orEmpty()
        }

        val hasExpatLegacyField = extractValueByLabels(
            pmtAndAllowanceLines,
            listOf("পরিবারে প্রবাসী সদস্য আছে কি", "প্রবাসী সদস্য আছে কিনা")
        ).ifBlank {
            if (allowanceInfo.totalAbroadCount > 0 || receivedRemittance2Yrs.contains("হ্যাঁ")) "হ্যাঁ" else receivedRemittance2Yrs
        }

        // 7. Fund Usage & Entry Operator
        val (fundItems, totalFundTaka) = extractFundUsage(fundUsageLines)
        val operatorInfo = EntryOperatorInfo(
            name = extractValueByLabels(entryOperatorLines, listOf("এন্ট্রি ব্যবহারকারীর নাম")),
            mobile = extractValueByLabels(entryOperatorLines, listOf("এন্ট্রি ব্যবহারকারীর মোবাইল")),
            userId = extractValueByLabels(entryOperatorLines, listOf("ইউজার আইডি"))
        )

        return ExtractedFamilyCardData(
            programInfo = ProgramOverviewInfo(
                mainProgram = mainProgram,
                subProgram = subProgram,
                familyMemberCount = familyMemberCountHeader.ifBlank { hhMemberCount }
            ),
            applicant = ApplicantInfo(
                applicationId = applicationId,
                applicationDate = applicationDate,
                nameBangla = nameBangla,
                nameEnglish = nameEnglish,
                nid = nid,
                nationality = nationality,
                dateOfBirth = dob,
                age = age,
                mobile = mobile,
                email = email,
                fatherNameBangla = fatherNameBangla,
                fatherNameEnglish = fatherNameEnglish,
                motherNameBangla = motherNameBangla,
                motherNameEnglish = motherNameEnglish,
                spouseName = spouseName,
                gender = gender,
                maritalStatus = maritalStatus,
                religion = religion,
                profession = applicantProfession,
                highestEducation = hhHeadEdu,
                presentAddress = collAddressStr,
                permanentAddress = permAddressStr
            ),
            permanentAddressDetails = permAddress,
            collectionAddressDetails = collAddress,
            bankMfsInfo = bankInfo,
            householdInfo = HouseholdSocioEconomicInfo(
                householdMemberCount = hhMemberCount,
                childCount = hhChildCount,
                householdHeadAgeRange = hhHeadAge,
                householdHeadEducation = hhHeadEdu,
                spouseEducation = spouseEdu,
                agriOrNonAgriDayLaborerStatus = agriLaborStatus,
                landOwnershipDecimal = totalLandOwnership,
                receivedRemittanceLast2Years = receivedRemittance2Yrs,
                mainWallMaterial = wallMaterial,
                hasSeparateDiningRoom = separateDining,
                hasSeparateKitchen = separateKitchen,
                drinkingWaterSource = waterSource,
                toiletType = toiletType,
                ownedAssetsRaw = ownedAssetsRaw,
                detectedAssets = detectedAssets,
                mainRoofMaterial = roofMaterial,
                totalRoomsCount = totalRooms,
                residenceAreaType = residenceLocation,
                mainFloorMaterial = floorMaterial,
                mainCookingFuel = cookingFuel,
                electricitySource = electricitySource,
                landOrHouseOwnershipType = landOwnershipType,
                isEthnicMinority = ethnicMinority,
                monthlyLpgUsageKg = lpgUsage,
                monthlyDieselUsageKg = dieselUsage,
                primaryLivelihoodProfession = primaryLivelihood,
                hasExpatriateMemberField = hasExpatLegacyField,
                remittanceOrAbroadDetails = receivedRemittance2Yrs,
                houseType = wallMaterial,
                monthlyFamilyIncome = totalFundTaka
            ),
            additionalAllowanceInfo = allowanceInfo,
            familyMembers = familyMembers,
            fundUsageItems = fundItems,
            totalFundUsageTaka = totalFundTaka,
            entryOperatorInfo = operatorInfo,
            rawFullText = fullLinesText,
            pageCount = 1
        )
    }

    private fun mergeBrokenMultilineQuestions(lines: List<String>): List<String> {
        val merged = mutableListOf<String>()
        val continuationPrefixes = listOf(
            "হিসেবে নিয়োজিত আছেন?", "হিসেবে নিয়োজিত আছেন?",
            "পরিমাণ কত?", "পরিমান কত?",
            "কারো নিকট হতে কোন অর্থ গ্রহণ করেছে কি?",
            "ব্যবহার করে?",
            "কোনটি আছে ? (একাধিক উত্তর হতে পারে)", "কোনটি আছে ?",
            "তৈরী",
            "গ্যাস হলে প্রতিমাসে এলপিজি গ্যাস ব্যবহারের", "পরিমান",
            "আনুমানিক পরিমাণ",
            "আর্থিক সুবিধা গ্রহণ করেছেন",
            "মোবাইল ব্যাংকিং-এ কোন হিসাব নম্বর আছে কি?",
            "কর্মসূচির আওতায়", "কর্মসূচির আওতায়",
            "প্রশিক্ষণ/আর্থিক সুবিধা"
        )

        var i = 0
        while (i < lines.size) {
            var current = lines[i]
            while (i + 1 < lines.size) {
                val next = lines[i + 1]
                val matchingPrefix = continuationPrefixes.firstOrNull { next.startsWith(it) }
                if (matchingPrefix != null) {
                    val afterPrefix = next.substring(matchingPrefix.length).trimStart(' ', ':')
                    current = if (afterPrefix.isNotBlank()) {
                        "$current $matchingPrefix : $afterPrefix"
                    } else {
                        "$current $matchingPrefix"
                    }
                    i++
                } else {
                    break
                }
            }
            merged.add(current)
            i++
        }
        return merged
    }

    private fun sliceSectionLines(
        lines: List<String>,
        startMarkers: List<String>,
        endMarkers: List<String>
    ): List<String> {
        val startIndex = lines.indexOfFirst { line ->
            startMarkers.any { marker -> line.contains(marker) }
        }
        if (startIndex < 0) return emptyList()

        val result = mutableListOf<String>()
        for (i in (startIndex + 1) until lines.size) {
            val line = lines[i]
            if (endMarkers.any { marker -> line.contains(marker) }) {
                break
            }
            result.add(line)
        }
        return result
    }

    private fun extractDualAddresses(lines: List<String>): Pair<AddressDetails, AddressDetails> {
        val divisions = mutableListOf<String>()
        val districts = mutableListOf<String>()
        val upazilas = mutableListOf<String>()
        val unions = mutableListOf<String>()
        val wards = mutableListOf<String>()
        val addrs = mutableListOf<String>()
        val postCodes = mutableListOf<String>()

        for (line in lines) {
            if (line.contains("ব্যাংক / এমএফএস তথ্য")) break
            when {
                line.startsWith("বিভাগ") -> divisions.add(line.substringAfter("বিভাগ").trim(' ', ':'))
                line.startsWith("জেলা") -> districts.add(line.substringAfter("জেলা").trim(' ', ':'))
                line.startsWith("উপজেলা") -> upazilas.add(line.substringAfter("উপজেলা").trim(' ', ':'))
                line.startsWith("ইউনিয়ন") || line.startsWith("ইউনিয়ন") ->
                    unions.add(line.substringAfter("ইউনি").substringAfter("ন").trim(' ', ':'))
                line.startsWith("ওয়ার্ড") || line.startsWith("ওয়ার্ড") ->
                    wards.add(line.substringAfter("য়ার্ড").substringAfter("য়ার্ড").trim(' ', ':'))
                line.startsWith("ঠিকানা") && !line.contains("স্থায়ী") && !line.contains("স্থায়ী") && !line.contains("সংগ্রহের") ->
                    addrs.add(line.substringAfter("ঠিকানা").trim(' ', ':'))
                line.startsWith("পোস্ট কোড") -> postCodes.add(line.substringAfter("পোস্ট কোড").trim(' ', ':'))
            }
        }

        val perm = AddressDetails(
            division = divisions.getOrNull(0).orEmpty(),
            district = districts.getOrNull(0).orEmpty(),
            upazila = upazilas.getOrNull(0).orEmpty(),
            unionName = unions.getOrNull(0).orEmpty(),
            ward = wards.getOrNull(0).orEmpty(),
            addressLine = addrs.getOrNull(0).orEmpty(),
            postCode = postCodes.getOrNull(0).orEmpty()
        )
        val coll = AddressDetails(
            division = divisions.getOrNull(1) ?: perm.division,
            district = districts.getOrNull(1) ?: perm.district,
            upazila = upazilas.getOrNull(1) ?: perm.upazila,
            unionName = unions.getOrNull(1) ?: perm.unionName,
            ward = wards.getOrNull(1) ?: perm.ward,
            addressLine = addrs.getOrNull(1) ?: perm.addressLine,
            postCode = postCodes.getOrNull(1) ?: perm.postCode
        )
        return perm to coll
    }

    private fun extractFamilyMembers(
        lines: List<String>,
        fallbackApplicantName: String
    ): List<FamilyMemberInfo> {
        // First check for DSS multi-row member blocks: "সদস্য ১ — হামিদা বেগম / HAMIDA BEGUM"
        val memberBlockHeaderRegex = Regex("^সদস্য\\s+([১-৯1-9][০-৯0-9]?)\\s*[—\\-:]\\s*(.+)$")
        val headerIndices = mutableListOf<Triple<Int, Int, String>>()

        for (i in lines.indices) {
            val line = lines[i].trim()
            val m = memberBlockHeaderRegex.find(line)
            if (m != null) {
                val serial = parseBanglaOrEnglishInt(m.groupValues[1]) ?: (headerIndices.size + 1)
                headerIndices.add(Triple(i, serial, m.groupValues[2].trim()))
            }
        }

        if (headerIndices.isNotEmpty()) {
            val members = mutableListOf<FamilyMemberInfo>()
            for (idx in headerIndices.indices) {
                val (startLineIdx, serial, headerRest) = headerIndices[idx]
                val endLineIdx = if (idx + 1 < headerIndices.size) {
                    headerIndices[idx + 1].first
                } else {
                    val stopIdx = lines.withIndex().indexOfFirst { (lineIdx, l) ->
                        lineIdx > startLineIdx && (l.contains("অর্থ ব্যবহার") || l.contains("এন্ট্রি ব্যবহারকারীর তথ্য"))
                    }
                    if (stopIdx >= 0) stopIdx else lines.size
                }

                val blockLines = lines.subList(startLineIdx + 1, endLineIdx)
                val headerParts = headerRest.split("/").map { it.trim() }
                val headerNameBn = headerParts.getOrNull(0).orEmpty()
                val headerNameEn = headerParts.getOrNull(1).orEmpty()

                val nameBn = extractValueByLabels(blockLines, listOf("নাম (বাংলা)")).ifBlank { headerNameBn }
                val nameEn = extractValueByLabels(blockLines, listOf("নাম (ইংরেজি)")).ifBlank { headerNameEn }
                val verifType = extractValueByLabels(blockLines, listOf("যাচাইকরণের ধরন"))
                val nidValue = extractMemberNidFromBlock(blockLines)
                val dob = extractValueByLabels(blockLines, listOf("জন্ম তারিখ"))
                val relation = extractValueByLabels(blockLines, listOf("সম্পর্ক"))
                val fatherBn = extractValueByLabels(blockLines, listOf("পিতার নাম (বাংলা)", "পিতার নাম"))
                val motherBn = extractValueByLabels(blockLines, listOf("মাতার নাম (বাংলা)", "মাতার নাম"))
                val gender = extractValueByLabels(blockLines, listOf("লিঙ্গ"))
                val marital = extractPersonalMaritalStatus(blockLines)
                val religion = extractValueByLabels(blockLines, listOf("ধর্ম"))
                val mobile = extractValueByLabels(blockLines, listOf("মোবাইল"))
                val prof = extractExactLabelValue(blockLines, "পেশা", excludePrefix = "জীবিকার")
                val annualIncome = extractValueByLabels(blockLines, listOf("আনুমানিক বার্ষিক আয়", "আনুমানিক বার্ষিক আয়"))
                val govEmp = extractValueByLabels(blockLines, listOf("সরকারি চাকরিজীবী"))
                val disabled = extractExactLabelValue(blockLines, "প্রতিবন্ধী", excludeSuffix = "র ধরন")
                val disabilityType = extractValueByLabels(blockLines, listOf("প্রতিবন্ধীর ধরন"))
                val literacy = extractValueByLabels(blockLines, listOf("সাক্ষরতা"))
                val student = extractValueByLabels(blockLines, listOf("বর্তমানে শিক্ষার্থী"))
                val education = extractValueByLabels(blockLines, listOf("শিক্ষাগত যোগ্যতা"))
                val health = extractValueByLabels(blockLines, listOf("স্বাস্থ্য অবস্থা"))
                val livelihoodProf = extractValueByLabels(blockLines, listOf("জীবিকার পেশা"))
                val safetyNet = extractValueByLabels(
                    blockLines,
                    listOf("সামাজিক নিরাপত্তা কর্মসূচির আওতায়", "সামাজিক নিরাপত্তা কর্মসূচির আওতায়", "সামাজিক নিরাপত্তা")
                )
                val dssSupport = extractValueByLabels(
                    blockLines,
                    listOf("ডিএসএস প্রশিক্ষণ/আর্থিক সুবিধা", "ডিএসএস")
                )
                val govCard = extractValueByLabels(blockLines, listOf("সরকারি কার্ডের ধরন"))

                val isExpat = prof.contains("প্রবাস") || livelihoodProf.contains("প্রবাস")

                members.add(
                    FamilyMemberInfo(
                        serial = serial,
                        headerTitle = "সদস্য $serial — $headerRest",
                        nameBangla = nameBn,
                        nameEnglish = nameEn,
                        verificationType = verifType,
                        relationWithApplicant = relation,
                        nidOrBirthReg = nidValue,
                        dateOfBirthOrAge = dob,
                        fatherNameBangla = fatherBn,
                        motherNameBangla = motherBn,
                        gender = gender,
                        maritalStatus = marital,
                        religion = religion,
                        mobile = mobile,
                        profession = prof,
                        livelihoodProfession = livelihoodProf,
                        estimatedAnnualIncome = annualIncome,
                        isGovEmployee = govEmp,
                        isDisabled = disabled,
                        disabilityType = disabilityType,
                        literacy = literacy,
                        isCurrentStudent = student,
                        highestEducation = education,
                        healthCondition = health,
                        underSocialSafetyNet = safetyNet,
                        receivedDssTrainingOrSupport = dssSupport,
                        govCardType = govCard,
                        isExpatriateOrAbroad = isExpat,
                        monthlyIncome = annualIncome,
                        rawRowText = blockLines.joinToString(" | ")
                    )
                )
            }
            return members.sortedBy { it.serial }
        }

        // Fallback for compact single-row member tables (e.g. "১. মোছাঃ রাবেয়া খাতুন নিজ ...")
        return extractCompactRowFamilyMembers(lines, fallbackApplicantName)
    }

    private fun extractMemberNidFromBlock(blockLines: List<String>): String {
        for (line in blockLines) {
            if (line.contains("এনআইডি নম্বর") || line.contains("জন্ম নিবন্ধন")) {
                val digitsMatch = Regex("([০-৯0-9]{10,17})").find(line)
                if (digitsMatch != null) return digitsMatch.value
            }
        }
        return Regex("\\b([০-৯0-9]{10,17})\\b").find(blockLines.joinToString(" "))?.value.orEmpty()
    }

    private fun extractExactLabelValue(
        lines: List<String>,
        label: String,
        excludePrefix: String = "",
        excludeSuffix: String = ""
    ): String {
        for (line in lines) {
            if (excludePrefix.isNotBlank() && line.contains("$excludePrefix $label")) continue
            if (excludeSuffix.isNotBlank() && line.contains("$label$excludeSuffix")) continue
            if (line.startsWith(label)) {
                val rem = line.substring(label.length).trimStart(' ', ':', '-', '|').trim()
                val cleaned = trimTrailingSecondColumnLabel(rem)
                if (cleaned.isNotBlank()) return cleaned
            }
        }
        return ""
    }

    private fun extractCompactRowFamilyMembers(
        lines: List<String>,
        applicantNameBangla: String
    ): List<FamilyMemberInfo> {
        val members = mutableListOf<FamilyMemberInfo>()
        val maritalTokens = listOf("অবিবাহিত", "বিবাহিত", "বিধবা", "বিপত্নীক", "তালাকপ্রাপ্ত", "স্বামী পরিত্যক্তা")
        val relationTokens = listOf("নিজ", "আবেদনকারী", "স্বামী", "স্ত্রী", "পুত্র", "কন্যা", "ছেলে", "মেয়ে", "মেয়ে", "পিতা", "মাতা", "ভাই", "বোন")
        val genderTokens = listOf("পুরুষ", "মহিলা", "নারী", "হিজড়া", "তৃতীয় লিঙ্গ")
        val expatriateTokens = listOf("প্রবাসী", "বিদেশ", "প্রবাস", "রেমিট্যান্স")

        val rowSerialRegex = Regex("^([১-৯1-9][০-৯0-9]?)\\s*[.):|-]\\s+(.+)$")
        for (line in lines) {
            val rowMatch = rowSerialRegex.find(line.trim()) ?: continue
            if (relationTokens.none { line.contains(it) }) continue

            val serial = parseBanglaOrEnglishInt(rowMatch.groupValues[1]) ?: (members.size + 1)
            val rowBody = rowMatch.groupValues[2].trim()
            val foundMarital = maritalTokens.firstOrNull { rowBody.contains(it) }.orEmpty()
            val foundRelation = relationTokens.firstOrNull { rowBody.contains(it) }.orEmpty()
            val foundGender = genderTokens.firstOrNull { rowBody.contains(it) }.orEmpty()
            val foundNid = Regex("\\b([০-৯0-9]{10,17})\\b").find(rowBody)?.value.orEmpty()
            val isExpat = expatriateTokens.any { rowBody.contains(it) }
            val tokens = rowBody.split(Regex("\\s{2,}|\\|")).map { it.trim() }.filter { it.isNotBlank() }
            val prof = tokens.lastOrNull().orEmpty()
            val nameCandidate = if (foundRelation.isNotBlank() && rowBody.indexOf(foundRelation) > 1) {
                rowBody.substring(0, rowBody.indexOf(foundRelation)).trim(' ', '|', '-', ',')
            } else {
                tokens.firstOrNull().orEmpty().ifBlank { applicantNameBangla }
            }

            members.add(
                FamilyMemberInfo(
                    serial = serial,
                    nameBangla = nameCandidate,
                    relationWithApplicant = foundRelation,
                    nidOrBirthReg = foundNid,
                    gender = foundGender,
                    maritalStatus = foundMarital,
                    profession = prof,
                    livelihoodProfession = prof,
                    isExpatriateOrAbroad = isExpat,
                    rawRowText = line.trim()
                )
            )
        }
        return members
    }

    private fun extractFundUsage(lines: List<String>): Pair<List<FundUsageItem>, String> {
        val items = mutableListOf<FundUsageItem>()
        var total = ""
        for (line in lines) {
            if (line.contains("ব্যবহারের খাত") || line.contains("পরিমাণ (টাকা)")) continue
            val parts = line.split(Regex("\\s{2,}|:")).map { it.trim() }.filter { it.isNotBlank() }
            if (line.startsWith("মোট")) {
                total = parts.getOrNull(1) ?: line.substringAfter("মোট").trim(' ', ':')
            } else if (parts.size >= 2) {
                items.add(
                    FundUsageItem(
                        sector = parts[0],
                        amountTaka = parts[1],
                        remarks = parts.getOrNull(2).orEmpty()
                    )
                )
            }
        }
        return items to total
    }

    private fun extractPersonalMaritalStatus(lines: List<String>): String {
        val statusKeywords = listOf(
            "অবিবাহিত", "বিবাহিত", "বিধবা", "বিপত্নীক", "তালাকপ্রাপ্ত", "স্বামী পরিত্যক্তা", "পৃথক"
        )
        for (line in lines) {
            if (line.contains("বৈবাহিক অবস্থা")) {
                val afterColon = line.substringAfter("বৈবাহিক অবস্থা").trimStart(':', '：', '-', ' ')
                for (kw in statusKeywords) {
                    if (afterColon.contains(kw)) return kw
                }
                if (afterColon.isNotBlank()) return afterColon.take(30).trim()
            }
        }
        return ""
    }

    private fun extractValueByLabels(lines: List<String>, labels: List<String>): String {
        for (i in lines.indices) {
            val line = lines[i]
            for (label in labels) {
                val idx = line.indexOf(label, ignoreCase = true)
                if (idx >= 0) {
                    val remainder = line.substring(idx + label.length)
                        .trimStart(' ', ':', '：', '-', '–', '|', '\t', '?')
                        .trim()
                    val cleanedRemainder = trimTrailingSecondColumnLabel(remainder)
                    if (cleanedRemainder.isNotBlank() && !isPureLabel(cleanedRemainder, labels)) {
                        return cleanedRemainder
                    }
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trimStart(' ', ':', '：', '-', '|').trim()
                        if (nextLine.isNotBlank() && !isPureLabel(nextLine, labels) && !nextLine.contains(":")) {
                            return nextLine
                        }
                    }
                }
            }
        }
        return ""
    }

    private fun trimTrailingSecondColumnLabel(value: String): String {
        val secondaryLabels = listOf(
            "আবেদনের তারিখ", "পিতার নাম (ইংরেজি)", "পিতার নাম (বাংলা)",
            "মাতার নাম (ইংরেজি)", "মাতার নাম (বাংলা)", "নাম (ইংরেজি)",
            "এনআইডি নম্বর", "জন্ম তারিখ", "সম্পর্ক", "বৈবাহিক অবস্থা",
            "মোবাইল", "আনুমানিক বার্ষিক আয়", "আনুমানিক বার্ষিক আয়",
            "প্রতিবন্ধী", "সাক্ষরতা", "শিক্ষাগত যোগ্যতা", "জীবিকার পেশা",
            "ডিএসএস প্রশিক্ষণ/আর্থিক সুবিধা", "ডিএসএস"
        )
        var result = value
        for (sec in secondaryLabels) {
            val pos = result.indexOf(sec)
            if (pos > 1) {
                result = result.substring(0, pos).trim()
            }
        }
        return result.trimEnd('|', ',', ';', ':', ' ')
    }

    private fun isPureLabel(candidate: String, knownLabels: List<String>): Boolean {
        return knownLabels.any { candidate.equals(it, ignoreCase = true) }
    }

    private fun detectSixAssets(ownedAssetsRaw: String, fullText: String): List<String> {
        val detected = mutableListOf<String>()
        val negativeIndicators = listOf("নাই", "নেই", "কোনটিই নেই", "প্রযোজ্য নয়", "প্রযোজ্য নয়", "শূন্য", "none")

        val searchScope = ownedAssetsRaw.ifBlank {
            fullText.lines().filter {
                it.contains("পরিসম্পদ") || it.contains("মোবাইল ফোন") || it.contains("বৈদ্যুতিক পাখা")
            }.joinToString(" ")
        }

        if (searchScope.isBlank()) return emptyList()
        val trimmedScope = searchScope.trim()
        if (negativeIndicators.any { trimmedScope.equals(it, ignoreCase = true) }) {
            return emptyList()
        }

        for ((canonicalName, keywords) in SIX_DESIGNATED_ASSETS) {
            val matchedKeyword = keywords.firstOrNull { kw ->
                searchScope.contains(kw, ignoreCase = true)
            }
            if (matchedKeyword != null) {
                val negatedNearby = negativeIndicators.any { neg ->
                    searchScope.contains("$matchedKeyword $neg") ||
                        searchScope.contains("$matchedKeyword: $neg")
                }
                if (!negatedNearby) {
                    detected.add(canonicalName)
                }
            }
        }
        return detected
    }

    fun parseBanglaOrEnglishInt(input: String): Int? {
        val mapped = input.map { ch ->
            when (ch) {
                in '০'..'৯' -> ('0'.code + (ch.code - '০'.code)).toChar()
                else -> ch
            }
        }.joinToString("")
        return mapped.trim().toIntOrNull()
    }
}
