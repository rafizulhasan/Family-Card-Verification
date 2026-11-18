package com.example

import com.example.data.BengaliPdfUnicodeDecoder
import com.example.data.OcrFamilyCardParser
import com.example.data.OfflineFamilyCardExtractor
import com.example.data.PipelineExtractionOutcome
import com.example.domain.FamilyCardValidator
import com.example.model.CriterionCheckStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun bengaliVisualDecoder_reordersPreBaseVowelsAndPuaLigatures() {
        val rawVisual = "েমাহাম্মদ িবলাল"
        val decoded = BengaliPdfUnicodeDecoder.decodeVisualBengali(rawVisual)
        assertEquals("মোহাম্মদ বিলাল", decoded)
    }

    @Test
    fun realDssFamilyCardPdf_extractsAllFieldsAndFlagsNoLandWarning() {
        val pdfFile = listOf(
            File("../public/assets/aistudio/4f178da7-256a-487f-8f09-074acf1963f7_80cd5cdd.pdf"),
            File("public/assets/aistudio/4f178da7-256a-487f-8f09-074acf1963f7_80cd5cdd.pdf")
        ).firstOrNull { it.exists() } ?: return

        val pdfBytes = pdfFile.readBytes()
        val positioned = OfflineFamilyCardExtractor.extractPositionedTextFromPdf(pdfBytes)
        assertTrue(positioned.isNotEmpty())

        val extracted = OfflineFamilyCardExtractor.parseFromPositionedItems(positioned)
        assertEquals("৭৭৯১৮৬৩৭৩৫", extracted.applicant.applicationId)
        assertEquals("হামিদা বেগম", extracted.applicant.nameBangla)
        assertEquals("HAMIDA BEGUM", extracted.applicant.nameEnglish)
        assertEquals("৭৩৪৫৮০৫৫৪৮", extracted.applicant.nid)
        assertEquals("বিবাহিত", extracted.applicant.maritalStatus)
        assertEquals("কোন জমি নেই", extracted.householdInfo.landOwnershipDecimal)
        assertEquals("স্যানিটারি/ উন্নত ল্যাট্রিন", extracted.householdInfo.toiletType)
        assertEquals("মোবাইল ফোন, বৈদ্যুতিক পাখা, খাট", extracted.householdInfo.ownedAssetsRaw)
        assertEquals("নিজস্ব", extracted.householdInfo.landOrHouseOwnershipType)
        assertEquals(2, extracted.familyMembers.size)
        assertEquals("হামিদা বেগম", extracted.familyMembers[0].nameBangla)
        assertEquals("বিবাহিত", extracted.familyMembers[0].maritalStatus)
        assertEquals("গৃহিণী", extracted.familyMembers[0].livelihoodProfession)
        assertEquals("আ: কুদ্দুছ মাতুব্বর", extracted.familyMembers[1].nameBangla)

        val criteria = FamilyCardValidator.validateAllCriteria(extracted)
        assertEquals(6, criteria.size)
        // Criterion 1: Completeness -> PASS
        assertEquals(CriterionCheckStatus.PASS, criteria[0].status)
        // Criterion 2: Marital Status (বিবাহিত vs বিবাহিত) -> PASS
        assertEquals(CriterionCheckStatus.PASS, criteria[1].status)
        // Criterion 3: Expatriate vs Livelihood -> PASS
        assertEquals(CriterionCheckStatus.PASS, criteria[2].status)
        // Criterion 4: Total Land Ownership ('কোন জমি নেই') -> WARNING
        assertEquals(CriterionCheckStatus.WARNING, criteria[3].status)
        assertTrue(criteria[3].summaryBangla.contains("কোন জমি নেই"))
        // Criterion 5: Toilet Type ('স্যানিটারি/ উন্নত ল্যাট্রিন') -> PASS
        assertEquals(CriterionCheckStatus.PASS, criteria[4].status)
        // Criterion 6: Designated Assets ('মোবাইল ফোন, বৈদ্যুতিক পাখা, খাট') -> PASS
        assertEquals(CriterionCheckStatus.PASS, criteria[5].status)
    }

    @Test
    fun validator_passesWhenAllSixCriteriaAreConsistent() {
        val sampleText = """
            আবেদন আইডি : ৭৭৯১৮৬৩৭৩৫
            ব্যক্তিগত তথ্য
            নাম (বাংলা) : হামিদা বেগম
            নাম (ইংরেজি) : HAMIDA BEGUM
            এনআইডি নম্বর : ৭৩৪৫৮০৫৫৪৮
            বৈবাহিক অবস্থা : বিবাহিত
            পিএমটি তথ্য
            খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত? : ৫ শতাংশ
            গত ২ বছরে আপনার খানা বিদেশে অবস্থানরত কারো নিকট হতে কোন অর্থ গ্রহণ করেছে কি? : না
            খানার সদস্যগণ কি ধরণের পায়খানা/টয়লেট ব্যবহার করে? : স্যানিটারি/ উন্নত ল্যাট্রিন
            খানার মালিকানাধীন নিম্নের পরিসম্পদ গুলোর কোনটি আছে ? (একাধিক উত্তর হতে পারে) : মোবাইল ফোন, বৈদ্যুতিক পাখা, খাট
            ভূমির বা বসতঘরের মালিকানা ধরণ : নিজস্ব
            অতিরিক্ত ভাতার তথ্য
            বিদেশে বসবাসরত পরিবারের পুরুষ সদস্য সংখ্যা : ০
            বিদেশে বসবাসরত পরিবারের মহিলা সদস্য সংখ্যা : ০
            বিদেশে বসবাসরত পরিবারের হিজড়া সদস্য সংখ্যা : ০
            পরিবারের সদস্য
            সদস্য ১ — হামিদা বেগম / HAMIDA BEGUM
            নাম (বাংলা) : হামিদা বেগম
            এনআইডি নম্বর : ৭৩৪৫৮০৫৫৪৮
            সম্পর্ক : নিজ
            বৈবাহিক অবস্থা : বিবাহিত
            জীবিকার পেশা : গৃহিণী
        """.trimIndent()

        val extracted = OcrFamilyCardParser.parseFromOcrText(sampleText)
        val report = FamilyCardValidator.buildVerificationReport(
            PipelineExtractionOutcome(
                extractedData = extracted,
                engineUsedLabel = "ইউনিট টেস্ট",
                usedOcrFallback = false,
                extractedCharCount = sampleText.length
            )
        )

        assertTrue(report.isAllConsistent)
        assertEquals(6, report.passCount)
        assertEquals(0, report.warningCount)
    }

    @Test
    fun validator_flagsWarningsOnMaritalExpatNoLandPitToiletAndAssets() {
        val sampleText = """
            ব্যক্তিগত তথ্য
            নাম (বাংলা) : মোঃ কামরুল হাসান
            এনআইডি নম্বর : ১৯৯০২৬৯৮৮৭৭৬৬
            বৈবাহিক অবস্থা : অবিবাহিত
            পিএমটি তথ্য
            খানার সকল সদস্যের মালিকানাধীন মোট জমির পরিমাণ কত? : কোন জমি নেই
            গত ২ বছরে আপনার খানা বিদেশে অবস্থানরত কারো নিকট হতে কোন অর্থ গ্রহণ করেছে কি? : হ্যাঁ
            খানার সদস্যগণ কি ধরণের পায়খানা/টয়লেট ব্যবহার করে? : পিট ল্যাট্রিন (গর্ত)
            খানার মালিকানাধীন নিম্নের পরিসম্পদ গুলোর কোনটি আছে ? (একাধিক উত্তর হতে পারে) : মোটরসাইকেল, ফ্রিজ
            অতিরিক্ত ভাতার তথ্য
            বিদেশে বসবাসরত পরিবারের পুরুষ সদস্য সংখ্যা : ১
            পরিবারের সদস্য
            সদস্য ১ — মোঃ কামরুল হাসান / MD. KAMRUL HASAN
            নাম (বাংলা) : মোঃ কামরুল হাসান
            এনআইডি নম্বর : ১৯৯০২৬৯৮৮৭৭৬৬
            সম্পর্ক : নিজ
            বৈবাহিক অবস্থা : বিবাহিত
            জীবিকার পেশা : কৃষি
        """.trimIndent()

        val extracted = OcrFamilyCardParser.parseFromOcrText(sampleText)
        val results = FamilyCardValidator.validateAllCriteria(extracted)

        assertEquals(CriterionCheckStatus.PASS, results[0].status)
        assertEquals(CriterionCheckStatus.WARNING, results[1].status)
        assertEquals(CriterionCheckStatus.WARNING, results[2].status)
        assertEquals(CriterionCheckStatus.WARNING, results[3].status)
        assertEquals(CriterionCheckStatus.WARNING, results[4].status)
        assertEquals(CriterionCheckStatus.WARNING, results[5].status)
        assertFalse(results.all { it.status == CriterionCheckStatus.PASS })
    }
}
