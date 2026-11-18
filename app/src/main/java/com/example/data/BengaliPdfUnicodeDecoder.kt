package com.example.data

import java.text.Normalizer

/**
 * 100% Free & Offline Bengali Visual-to-Logical Unicode Reconstructor.
 */
object BengaliPdfUnicodeDecoder {

    private val kalpurushPuaMap: Map<Int, String> = mapOf(
        0xE002 to "্য", 0xE003 to "ক্ক", 0xE004 to "ক্ট", 0xE005 to "ক্ত", 0xE006 to "ক্ত্র",
        0xE007 to "ক্ত্ব", 0xE008 to "ক্ন", 0xE009 to "ক্ম", 0xE00A to "ক্র", 0xE00B to "ক্ল",
        0xE00C to "ক্ব", 0xE00D to "ক্ষ", 0xE00E to "ক্ষ্ণ", 0xE00F to "ক্ষ্ম", 0xE010 to "ক্ষ্র",
        0xE011 to "ক্ষ্ব", 0xE012 to "ক্স", 0xE013 to "খ্র", 0xE014 to "খ্ব", 0xE015 to "গু",
        0xE016 to "গ্গ", 0xE017 to "গ্দ", 0xE018 to "গ্ধ", 0xE019 to "গ্ধ্ব", 0xE01A to "গ্ধ্র",
        0xE01B to "গ্ন", 0xE01C to "গ্ম", 0xE01D to "গ্র", 0xE01E to "গ্রু", 0xE01F to "গ্রূ",
        0xE020 to "গ্ল", 0xE021 to "গ্ব", 0xE022 to "ঘ্ন", 0xE023 to "ঘ্র", 0xE024 to "ঘ্ব",
        0xE025 to "ঙ্ক", 0xE026 to "ঙ্ক্র", 0xE027 to "ঙ্ক্ষ", 0xE028 to "ঙ্ক্ষ্র", 0xE029 to "ঙ্খ",
        0xE02A to "ঙ্গ", 0xE02B to "ঙ্ঘ", 0xE02C to "ঙ্ঘ্র", 0xE02D to "ঙ্ম", 0xE02E to "ঙ্র",
        0xE02F to "চ্চ", 0xE030 to "চ্ছ", 0xE031 to "চ্ছ্র", 0xE032 to "চ্ছ্ব", 0xE033 to "চ্ঞ",
        0xE034 to "চ্ন", 0xE035 to "চ্র", 0xE036 to "চ্ব", 0xE037 to "ছ্র", 0xE038 to "ছ্ব",
        0xE039 to "জ্জ", 0xE03A to "জ্জ্ব", 0xE03B to "জ্ঝ", 0xE03C to "জ্ঞ", 0xE03D to "জ্র",
        0xE03E to "জ্ব", 0xE03F to "ঝ্র", 0xE040 to "ঞ্চ", 0xE041 to "ঞ্চ্ব", 0xE042 to "ঞ্ছ",
        0xE043 to "ঞ্জ", 0xE044 to "ঞ্ঝ", 0xE045 to "ঞ্ধ", 0xE046 to "ট্ট", 0xE047 to "ট্ট্র",
        0xE048 to "ট্ম", 0xE049 to "ট্র", 0xE04A to "ট্ব", 0xE04B to "ঠ্র", 0xE04C to "ড্গ",
        0xE04D to "ড্ড", 0xE04E to "ড্ম", 0xE04F to "ড্র", 0xE050 to "ড্ব", 0xE051 to "ঢ্র",
        0xE052 to "ণ্ট", 0xE053 to "ণ্ট্র", 0xE054 to "ণ্ঠ", 0xE055 to "ণ্ঠ্র", 0xE056 to "ণ্ড",
        0xE057 to "ণ্ড্র", 0xE058 to "ণ্ঢ", 0xE059 to "ণ্ঢ্র", 0xE05A to "ণ্ণ", 0xE05B to "ণ্ম",
        0xE05C to "ণ্র", 0xE05D to "ণ্ব", 0xE05E to "ত্ত", 0xE05F to "ত্ত্ব", 0xE060 to "ত্থ",
        0xE061 to "ত্ন", 0xE062 to "ত্ম", 0xE063 to "ত্র", 0xE064 to "ত্রু", 0xE065 to "ত্রূ",
        0xE066 to "ত্ল", 0xE067 to "ত্ব", 0xE068 to "থ্র", 0xE069 to "থ্রু", 0xE06A to "থ্রূ",
        0xE06B to "থ্ব", 0xE06C to "দ্গ", 0xE06D to "দ্ঘ", 0xE06E to "দ্দ", 0xE06F to "দ্দ্র",
        0xE070 to "দ্দ্ব", 0xE071 to "দ্ধ", 0xE072 to "দ্ধ্ব", 0xE073 to "দ্ন", 0xE074 to "দ্ভ",
        0xE075 to "দ্ভ্র", 0xE076 to "দ্ম", 0xE077 to "দ্র", 0xE078 to "দ্রু", 0xE079 to "দ্রূ",
        0xE07A to "দ্ব", 0xE07B to "ধ্ন", 0xE07C to "ধ্ম", 0xE07D to "ধ্র", 0xE07E to "ধ্রু",
        0xE07F to "ধ্রূ", 0xE080 to "ধ্ব", 0xE081 to "ন্ট", 0xE082 to "ন্ট্র", 0xE083 to "ন্ঠ",
        0xE084 to "ন্ড", 0xE085 to "ন্ড্র", 0xE087 to "ন্ত", 0xE088 to "ন্তু", 0xE089 to "ন্ত্র",
        0xE08A to "ন্ত্ব", 0xE08B to "ন্থ", 0xE08C to "ন্থ্র", 0xE08D to "ন্দ", 0xE08E to "ন্দ্র",
        0xE08F to "ন্দ্ব", 0xE090 to "ন্ধ", 0xE091 to "ন্ধ্ব", 0xE092 to "ন্ধ্র", 0xE093 to "ন্ন",
        0xE094 to "ন্ম", 0xE095 to "ন্র", 0xE096 to "ন্ব", 0xE097 to "ন্স", 0xE098 to "প্ট",
        0xE099 to "প্ত", 0xE09A to "প্তু", 0xE09B to "প্ত্র", 0xE09C to "প্ন", 0xE09D to "প্প",
        0xE09E to "প্ম", 0xE09F to "প্র", 0xE0A0 to "প্রু", 0xE0A1 to "প্রূ", 0xE0A2 to "প্ল",
        0xE0A3 to "প্স", 0xE0A4 to "ফ্ট", 0xE0A5 to "ফ্র", 0xE0A6 to "ফ্ল", 0xE0AE to "ব্ব",
        0xE0B0 to "ব্র", 0xE0B1 to "ব্রু", 0xE0B2 to "ব্রূ", 0xE0B3 to "ব্ল", 0xE0B4 to "ভ্র",
        0xE0B5 to "ভ্রু", 0xE0B6 to "ভ্রূ", 0xE0B7 to "ভ্ল", 0xE0B8 to "ম্ত", 0xE0B9 to "ম্দ",
        0xE0BA to "ম্ন", 0xE0BB to "ম্প", 0xE0BC to "ম্প্র", 0xE0BD to "ম্ফ", 0xE0BE to "ম্ফ্র",
        0xE0BF to "ম্ব", 0xE0C0 to "ম্ভ", 0xE0C1 to "ম্ভ্র", 0xE0C2 to "ম্ম", 0xE0C3 to "ম্র",
        0xE0C4 to "ম্ল", 0xE0C5 to "য্র", 0xE0C6 to "রু", 0xE0C7 to "রূ", 0xE0C8 to "ল্ক",
        0xE0C9 to "ল্গ", 0xE0CA to "ল্গু", 0xE0CB to "ল্ঘ", 0xE0CC to "ল্ট", 0xE0CD to "ল্ট্র",
        0xE0CE to "ল্ড", 0xE0CF to "ল্ড্র", 0xE0D0 to "ল্ঢ", 0xE0D1 to "ল্দ", 0xE0D2 to "ল্প",
        0xE0D3 to "ল্ফ", 0xE0D4 to "ল্ব", 0xE0D5 to "ল্ম", 0xE0D6 to "ল্র", 0xE0D7 to "ল্ল",
        0xE0D8 to "শু", 0xE0D9 to "শ্চ", 0xE0DA to "শ্ছ", 0xE0DB to "শ্ত", 0xE0DC to "শ্ন",
        0xE0DD to "শ্ম", 0xE0DE to "শ্ব", 0xE0DF to "শ্র", 0xE0E0 to "শ্রী", 0xE0E1 to "শ্রু",
        0xE0E2 to "শ্রূ", 0xE0E3 to "শ্ল", 0xE0E4 to "ষ্ক", 0xE0E5 to "ষ্ক্র", 0xE0E6 to "ষ্ট",
        0xE0E7 to "ষ্ট্র", 0xE0E8 to "ষ্ঠ", 0xE0E9 to "ষ্ণ", 0xE0EA to "ষ্প", 0xE0EB to "ষ্প্র",
        0xE0EC to "ষ্ফ", 0xE0ED to "ষ্র", 0xE0EE to "ষ্ব", 0xE0EF to "ষ্ম", 0xE0F0 to "স্ক",
        0xE0F1 to "স্ক্র", 0xE0F3 to "স্খ", 0xE0F4 to "স্ট", 0xE0F5 to "স্ট্র", 0xE0F7 to "স্ত",
        0xE0F8 to "স্তু", 0xE0F9 to "স্ত্র", 0xE0FA to "স্ত্ব", 0xE0FB to "স্থ", 0xE0FC to "স্ন",
        0xE0FD to "স্প", 0xE0FE to "স্প্র", 0xE100 to "স্প্ল", 0xE101 to "স্ফ", 0xE102 to "স্ব",
        0xE103 to "স্ম", 0xE104 to "স্র", 0xE105 to "স্রু", 0xE106 to "স্রূ", 0xE107 to "স্ল",
        0xE108 to "হু", 0xE109 to "হৃ", 0xE10A to "হ্ণ", 0xE10B to "হ্ন", 0xE10C to "হ্ব",
        0xE10D to "হ্ম", 0xE10E to "হ্র", 0xE10F to "হ্ল", 0xE110 to "ড়্গ", 0xE111 to "ড়্র",
        0xE112 to "ঢ়্র", 0xE113 to "য়্র", 0xE114 to "রু", 0xE115 to "রূ", 0xE116 to "রু",
        0xE117 to "রূ", 0xE118 to "র্র", 0xE11A to "ঁী", 0xE11B to "ঁৗ", 0xE11C to "ে",
        0xE11D to "ৈ", 0xE121 to "॥", 0xE122 to "ক্", 0xE123 to "খ্", 0xE124 to "গ্",
        0xE125 to "ঘ্", 0xE126 to "ঙ্", 0xE127 to "চ্", 0xE128 to "ছ্", 0xE129 to "জ্",
        0xE12A to "ঝ্", 0xE12B to "ঞ্", 0xE12C to "ট্", 0xE12D to "ঠ্", 0xE12E to "ড্",
        0xE12F to "ঢ্", 0xE130 to "ণ্", 0xE132 to "থ্", 0xE133 to "দ্", 0xE134 to "ধ্",
        0xE135 to "ন্", 0xE136 to "প্", 0xE137 to "ফ্", 0xE139 to "ভ্", 0xE13A to "ম্",
        0xE13B to "য্", 0xE13D to "ল্", 0xE13E to "ব্", 0xE13F to "শ্", 0xE140 to "ষ্",
        0xE141 to "স্", 0xE142 to "হ্", 0xE143 to "ড়্", 0xE144 to "ঢ়্", 0xE145 to "য়্",
        0xE147 to "র্", 0xE185 to "।", 0xE186 to "ু", 0xE187 to "ূ", 0xE188 to "ক",
        0xE189 to "খ", 0xE18A to "গ", 0xE18B to "ঘ", 0xE18C to "ঙ", 0xE18D to "চ",
        0xE18E to "ছ", 0xE18F to "জ", 0xE190 to "ঝ", 0xE191 to "ঞ", 0xE192 to "ট",
        0xE193 to "ঠ", 0xE194 to "ড", 0xE195 to "ঢ", 0xE196 to "ণ", 0xE197 to "ত",
        0xE198 to "থ", 0xE199 to "দ", 0xE19A to "ধ", 0xE19B to "ন", 0xE19C to "প",
        0xE19D to "ফ", 0xE19E to "ব", 0xE19F to "ভ", 0xE1A0 to "ম", 0xE1A1 to "য",
        0xE1A2 to "র", 0xE1A3 to "ল", 0xE1A4 to "শ", 0xE1A5 to "ষ", 0xE1A6 to "স",
        0xE1A7 to "হ", 0xE1A8 to "ড়", 0xE1A9 to "ঢ়", 0xE1AA to "য়", 0xE1AB to "র",
        0xE1AC to "র", 0xE1AD to "ক্ষ", 0xE1AF to "ক্ত্র", 0xE1B3 to "ম্ব্র", 0xE1B4 to "ণ্ন"
    )

    private val preBaseVowels = setOf('ি', 'ে', 'ৈ')
    private val postBaseVowels = setOf('া', 'ী', 'ু', 'ূ', 'ৃ', 'ো', 'ৌ', 'ৗ', 'ঁ', 'ং', 'ঃ')

    private fun isBengaliConsonant(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x0995..0x09B9) ||
            ch == 'ড়' || ch == 'ঢ়' || ch == 'য়' ||
            ch == 'ৎ' || ch == '\u09DC' || ch == '\u09DD' || ch == '\u09DF'
    }

    fun decodeVisualBengali(rawVisual: String): String {
        if (rawVisual.isEmpty()) return ""

        val expanded = StringBuilder(rawVisual.length + 16)
        for (ch in rawVisual) {
            val mapped = kalpurushPuaMap[ch.code]
            if (mapped != null) {
                expanded.append(mapped)
            } else {
                expanded.append(ch)
            }
        }

        val chars = expanded.toString().toMutableList()
        var i = 0
        while (i < chars.size) {
            val ch = chars[i]
            if (ch in preBaseVowels) {
                var j = i + 1
                if (j < chars.size && isBengaliConsonant(chars[j])) {
                    j++
                    while (j + 1 < chars.size && chars[j] == '্' && isBengaliConsonant(chars[j + 1])) {
                        j += 2
                    }
                    for (k in i until j - 1) {
                        chars[k] = chars[k + 1]
                    }
                    chars[j - 1] = ch
                    i = j
                    continue
                }
            }
            i++
        }

        var idx = 0
        while (idx < chars.size) {
            val code = chars[idx].code
            if (code == 0xE11E || code == 0xE11F) {
                var k = idx - 1
                while (k >= 0 && (chars[k] in preBaseVowels || chars[k] in postBaseVowels)) {
                    k--
                }
                if (k >= 0 && isBengaliConsonant(chars[k])) {
                    var startCluster = k
                    while (
                        startCluster - 2 >= 0 &&
                        chars[startCluster - 1] == '্' &&
                        isBengaliConsonant(chars[startCluster - 2])
                    ) {
                        startCluster -= 2
                    }
                    chars.removeAt(idx)
                    chars.add(startCluster, '্')
                    chars.add(startCluster, 'র')
                    idx++
                    continue
                } else {
                    chars[idx] = 'র'
                    chars.add(idx + 1, '্')
                    idx += 2
                    continue
                }
            }
            idx++
        }

        val logical = chars.joinToString("")
            .replace("ো", "ো")
            .replace("ৌ", "ৌ")
            .replace("ু্য", "্যু")
            .replace("ূ্য", "্যূ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return Normalizer.normalize(logical, Normalizer.Form.NFC)
    }
}
