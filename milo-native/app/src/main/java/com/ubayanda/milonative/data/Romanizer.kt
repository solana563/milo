package com.ubayanda.milonative.data

import com.atilika.kuromoji.ipadic.Tokenizer
import com.github.promeg.pinyinhelper.Pinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Readings for lyrics in other scripts: Japanese (kuromoji), Chinese (pinyin), Korean, Russian / Ukrainian / Serbian / Bulgarian / Belarusian /
 * Kyrgyz / Macedonian, Hindi and Punjabi. The tables and the rules are Mochy Music's (GPL-3.0, github.com/realidkroo/Mochy-Music, LyricsUtils.kt),
 * moved out of its lyrics object into one file for milo.
 */
object Romanizer {
    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(

        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ヂャ" to "ja", "ヂュ" to "ju", "ヂョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",

        // loanword sounds built with small kana, which used to leak into the reading as katakana
        "ティ" to "ti", "ディ" to "di", "トゥ" to "tu", "ドゥ" to "du", "テュ" to "tyu", "デュ" to "dyu",
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo", "フュ" to "fyu",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo", "イェ" to "ye",
        "ヴァ" to "va", "ヴィ" to "vi", "ヴェ" to "ve", "ヴォ" to "vo", "ヴュ" to "vyu",
        "シェ" to "she", "ジェ" to "je", "チェ" to "che",
        "ツァ" to "tsa", "ツィ" to "tsi", "ツェ" to "tse", "ツォ" to "tso",
        "クァ" to "kwa", "グァ" to "gwa",

        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヲ" to "o", "ン" to "n",

        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do",

        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo",
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po",
        "ヴ" to "vu",

        "ァ" to "a", "ィ" to "i", "ゥ" to "u", "ェ" to "e", "ォ" to "o",
        "ャ" to "ya", "ュ" to "yu", "ョ" to "yo", "ヮ" to "wa",
        "ヰ" to "i", "ヱ" to "e", "ヵ" to "ka", "ヶ" to "ke",
    )

    private val HANGUL_ROMAJA_MAP: Map<String, Map<String, String>> = mapOf(
        "cho" to mapOf(
            "ᄀ" to "g", "ᄁ" to "kk", "ᄂ" to "n", "ᄃ" to "d",
            "ᄄ" to "tt", "ᄅ" to "r", "ᄆ" to "m", "ᄇ" to "b",
            "ᄈ" to "pp", "ᄉ" to "s", "ᄊ" to "ss", "ᄋ" to "",
            "ᄌ" to "j", "ᄍ" to "jj", "ᄎ" to "ch", "ᄏ" to "k",
            "ᄐ" to "t", "ᄑ" to "p", "ᄒ" to "h"
        ),
        "jung" to mapOf(
            "ᅡ" to "a", "ᅢ" to "ae", "ᅣ" to "ya", "ᅤ" to "yae",
            "ᅥ" to "eo", "ᅦ" to "e", "ᅧ" to "yeo", "ᅨ" to "ye",
            "ᅩ" to "o", "ᅪ" to "wa", "ᅫ" to "wae", "ᅬ" to "oe",
            "ᅭ" to "yo", "ᅮ" to "u", "ᅯ" to "wo", "ᅰ" to "we",
            "ᅱ" to "wi", "ᅲ" to "yu", "ᅳ" to "eu", "ᅴ" to "eui",
            "ᅵ" to "i"
        ),
        "jong" to mapOf(
            "ᆨ" to "k", "ᆨᄋ" to "g", "ᆨᄂ" to "ngn", "ᆨᄅ" to "ngn", "ᆨᄆ" to "ngm", "ᆨᄒ" to "kh",
            "ᆩ" to "kk", "ᆩᄋ" to "kg", "ᆩᄂ" to "ngn", "ᆩᄅ" to "ngn", "ᆩᄆ" to "ngm", "ᆩᄒ" to "kh",
            "ᆪ" to "k", "ᆪᄋ" to "ks", "ᆪᄂ" to "ngn", "ᆪᄅ" to "ngn", "ᆪᄆ" to "ngm", "ᆪᄒ" to "kch",
            "ᆫ" to "n", "ᆫᄅ" to "ll", "ᆬ" to "n", "ᆬᄋ" to "nj", "ᆬᄂ" to "nn", "ᆬᄅ" to "nn",
            "ᆬᄆ" to "nm", "ᆬㅎ" to "nch", "ᆭ" to "n", "ᆭᄋ" to "nh", "ᆭᄅ" to "nn", "ᆮ" to "t",
            "ᆮᄋ" to "d", "ᆮᄂ" to "nn", "ᆮᄅ" to "nn", "ᆮᄆ" to "nm", "ᆮᄒ" to "th", "ᆯ" to "l",
            "ᆯᄋ" to "r", "ᆯᄂ" to "ll", "ᆯᄅ" to "ll", "ᆰ" to "k", "ᆰᄋ" to "lg", "ᆰᄂ" to "ngn",
            "ᆰᄅ" to "ngn", "ᆰᄆ" to "ngm", "ᆰᄒ" to "lkh", "ᆱ" to "m", "ᆱᄋ" to "lm", "ᆱᄂ" to "mn",
            "ᆱᄅ" to "mn", "ᆱᄆ" to "mm", "ᆱᄒ" to "lmh", "ᆲ" to "p", "ᆲᄋ" to "lb", "ᆲᄂ" to "mn",
            "ᆲᄅ" to "mn", "ᆲᄆ" to "mm", "ᆲᄒ" to "lph", "ᆳ" to "t", "ᆳᄋ" to "ls", "ᆳᄂ" to "nn",
            "ᆳᄅ" to "nn", "ᆳᄆ" to "nm", "ᆳᄒ" to "lsh", "ᆴ" to "t", "ᆴᄋ" to "lt", "ᆴᄂ" to "nn",
            "ᆴᄅ" to "nn", "ᆴᄆ" to "nm", "ᆴᄒ" to "lth", "ᆵ" to "p", "ᆵᄋ" to "lp", "ᆵᄂ" to "mn",
            "ᆵᄅ" to "mn", "ᆵᄆ" to "mm", "ᆵᄒ" to "lph", "ᆶ" to "l", "ᆶᄋ" to "lh", "ᆶᄂ" to "ll",
            "ᆶᄅ" to "ll", "ᆶᄆ" to "lm", "ᆶᄒ" to "lh", "ᆷ" to "m", "ᆷᄅ" to "mn", "ᆸ" to "p",
            "ᆸᄋ" to "b", "ᆸᄂ" to "mn", "ᆸᄅ" to "mn", "ᆸᄆ" to "mm", "ᆸᄒ" to "ph", "ᆹ" to "p",
            "ᆹᄋ" to "ps", "ᆹᄂ" to "mn", "ᆹᄅ" to "mn", "ᆹᄆ" to "mm", "ᆹᄒ" to "psh", "ᆺ" to "t",
            "ᆺᄋ" to "s", "ᆺᄂ" to "nn", "ᆺᄅ" to "nn", "ᆺᄆ" to "nm", "ᆺᄒ" to "sh", "ᆻ" to "t",
            "ᆻᄋ" to "ss", "ᆻᄂ" to "tn", "ᆻᄅ" to "tn", "ᆻᄆ" to "nm", "ᆻᄒ" to "th", "ᆼ" to "ng",
            "ᆽ" to "t", "ᆽᄋ" to "j", "ᆽᄂ" to "nn", "ᆽᄅ" to "nn", "ᆽᄆ" to "nm", "ᆽᄒ" to "ch",
            "ᆾ" to "t", "ᆾᄋ" to "ch", "ᆾᄂ" to "nn", "ᆾᄅ" to "nn", "ᆾᄆ" to "nm", "ᆾᄒ" to "ch",
            "ᆿ" to "k", "ᆿᄋ" to "k", "ᆿᄂ" to "ngn", "ᆿᄅ" to "ngn", "ᆿᄆ" to "ngm", "ᆿᄒ" to "kh",
            "ᇀ" to "t", "ᇀᄋ" to "t", "ᇀᄂ" to "nn", "ᇀᄅ" to "nn", "ᇀᄆ" to "nm", "ᇀᄒ" to "th",
            "ᇁ" to "p", "ᇁᄋ" to "p", "ᇁᄂ" to "mn", "ᇁᄅ" to "mn", "ᇁᄆ" to "mm", "ᇁᄒ" to "ph",
            "ᇂ" to "t", "ᇂᄋ" to "h", "ᇂᄂ" to "nn", "ᇂᄅ" to "nn", "ᇂᄆ" to "mm", "ᇂᄒ" to "t",
            "ᇂᄀ" to "k"
        )
    )

    private val DEVANAGARI_ROMAJI_MAP: Map<String, String> = mapOf(
        "अ" to "a", "आ" to "aa", "इ" to "i", "ई" to "ee", "उ" to "u", "ऊ" to "oo",
        "ऋ" to "ri", "ए" to "e", "ऐ" to "ai", "ओ" to "o", "औ" to "au",
        "क" to "k", "ख" to "kh", "ग" to "g", "घ" to "gh", "ङ" to "ng",
        "च" to "ch", "छ" to "chh", "ज" to "j", "झ" to "jh", "ञ" to "ny",
        "ट" to "t", "ठ" to "th", "ड" to "d", "ढ" to "dh", "ण" to "n",
        "त" to "t", "थ" to "th", "द" to "d", "ध" to "dh", "न" to "n",
        "प" to "p", "फ" to "ph", "ब" to "b", "भ" to "bh", "म" to "m",
        "य" to "y", "र" to "r", "ल" to "l", "व" to "v",
        "श" to "sh", "ष" to "sh", "स" to "s", "ह" to "h",
        "क्ष" to "ksh", "त्र" to "tr", "ज्ञ" to "gy", "श्र" to "shr",
        "ा" to "aa", "ि" to "i", "ी" to "ee", "ु" to "u", "ू" to "oo",
        "ृ" to "ri", "े" to "e", "ै" to "ai", "ो" to "o", "ौ" to "au",
        "ं" to "n", "ः" to "h", "ँ" to "n", "़" to "", "्" to "",
        "०" to "0", "१" to "1", "२" to "2", "३" to "3", "४" to "4",
        "५" to "5", "६" to "6", "७" to "7", "८" to "8", "९" to "9",
        "ॐ" to "Om", "ऽ" to "",
        "क़" to "q", "ख़" to "kh", "ग़" to "g", "ज़" to "z", "ड़" to "r", "ढ़" to "rh", "फ़" to "f", "य़" to "y",

        "क\u093C" to "q", "ख\u093C" to "kh", "ग\u093C" to "g", "ज\u093C" to "z", "ड\u093C" to "r", "ढ\u093C" to "rh", "फ\u093C" to "f", "य\u093C" to "y"
    )

    private val GURMUKHI_ROMAJI_MAP: Map<String, String> = mapOf(
        "ੳ" to "o", "ਅ" to "a", "ੲ" to "e", "ਸ" to "s", "ਹ" to "h",
        "ਕ" to "k", "ਖ" to "kh", "ਗ" to "g", "ਘ" to "gh", "ਙ" to "ng",
        "ਚ" to "ch", "ਛ" to "chh", "ਜ" to "j", "ਝ" to "jh", "ਞ" to "ny",
        "ਟ" to "t", "ਠ" to "th", "ਡ" to "d", "ਢ" to "dh", "ਣ" to "n",
        "ਤ" to "t", "ਥ" to "th", "ਦ" to "d", "ਧ" to "dh", "ਨ" to "n",
        "ਪ" to "p", "ਫ" to "ph", "ਬ" to "b", "ਭ" to "bh", "ਮ" to "m",
        "ਯ" to "y", "ਰ" to "r", "ਲ" to "l", "ਵ" to "v", "ੜ" to "r",
        "ਸ਼" to "sh", "ਖ਼" to "kh", "ਗ਼" to "g", "ਜ਼" to "z", "ਫ਼" to "f", "ਲ਼" to "l",
        "ਾ" to "aa", "ਿ" to "i", "ੀ" to "ee", "ੁ" to "u", "ੂ" to "oo",
        "ੇ" to "e", "ੈ" to "ai", "ੋ" to "o", "ੌ" to "au",
        "ੰ" to "n", "ਂ" to "n", "ੱ" to "", "੍" to "", "਼" to "",
        "ੴ" to "Ek Onkar",
        "੦" to "0", "੧" to "1", "੨" to "2", "੩" to "3", "੪" to "4",
        "੫" to "5", "੬" to "6", "੭" to "7", "੮" to "8", "੯" to "9"
    )

    private val GENERAL_CYRILLIC_ROMAJI_MAP: Map<String, String> = mapOf(
        "А" to "A", "Б" to "B", "В" to "V", "Г" to "G", "Ґ" to "G", "Д" to "D",
        "Ѓ" to "Ǵ", "Ђ" to "Đ", "Е" to "E", "Ё" to "Yo", "Є" to "Ye", "Ж" to "Zh",
        "З" to "Z", "Ѕ" to "Dz", "И" to "I", "І" to "I", "Ї" to "Yi", "Й" to "Y",
        "Ј" to "Y", "К" to "K", "Л" to "L", "Љ" to "Ly", "М" to "M", "Н" to "N",
        "Њ" to "Ny", "О" to "O", "П" to "P", "Р" to "R", "С" to "S", "Т" to "T",
        "Ћ" to "Ć", "У" to "U", "Ў" to "Ŭ", "Ф" to "F", "Х" to "Kh", "Ц" to "Ts",
        "Ч" to "Ch", "Џ" to "Dž", "Ш" to "Sh", "Щ" to "Shch", "Ъ" to "ʺ", "Ы" to "Y",
        "Ь" to "ʹ", "Э" to "E", "Ю" to "Yu", "Я" to "Ya",
        "Ѡ" to "O", "Ѣ" to "Ya", "Ѥ" to "Ye", "Ѧ" to "Ya", "Ѩ" to "Ya",
        "Ѫ" to "U", "Ѭ" to "Yu", "Ѯ" to "Ks", "Ѱ" to "Ps", "Ѳ" to "F",
        "Ѵ" to "I", "Ѷ" to "I", "Ғ" to "Gh", "Ҕ" to "G", "Җ" to "Zh",
        "Ҙ" to "Dz", "Қ" to "Q", "Ҝ" to "K", "Ҟ" to "K", "Ҡ" to "K",
        "Ң" to "Ng", "Ҥ" to "Ng", "Ҧ" to "P", "Ҩ" to "O", "Ҫ" to "S",
        "Ҭ" to "T", "Ү" to "U", "Ұ" to "U", "Ҳ" to "Kh", "Ҵ" to "Ts",
        "Ҷ" to "Ch", "Ҹ" to "Ch", "Һ" to "H", "Ҽ" to "Ch", "Ҿ" to "Ch",
        "Ќ" to "Ḱ", "Ө" to "Ö",

        "а" to "a", "б" to "b", "в" to "v", "г" to "g", "ґ" to "g", "д" to "d",
        "ѓ" to "ǵ", "ђ" to "đ", "е" to "e", "ё" to "yo", "є" to "ye", "ж" to "zh",
        "з" to "z", "ѕ" to "dz", "и" to "i", "і" to "i", "ї" to "yi", "й" to "y",
        "ј" to "y", "к" to "k", "л" to "l", "љ" to "ly", "м" to "m", "н" to "n",
        "њ" to "ny", "о" to "o", "п" to "p", "р" to "r", "с" to "s", "т" to "t",
        "ћ" to "ć", "у" to "u", "ў" to "ŭ", "ф" to "f", "х" to "kh", "ц" to "ts",
        "ч" to "ch", "џ" to "dž", "ш" to "sh", "щ" to "shch", "ъ" to "ʺ", "ы" to "y",
        "ь" to "ʹ", "э" to "e", "ю" to "yu", "я" to "ya",
        "ѡ" to "o", "ѣ" to "ya", "ѥ" to "ye", "ѧ" to "ya", "ѩ" to "ya",
        "ѫ" to "u", "ѭ" to "yu", "ѯ" to "ks", "ѱ" to "ps", "ѳ" to "f",
        "ѵ" to "i", "ѷ" to "i", "ғ" to "gh", "ҕ" to "g", "җ" to "zh",
        "ҙ" to "dz", "қ" to "q", "ҝ" to "k", "ҟ" to "k", "ҡ" to "k",
        "ң" to "ng", "ҥ" to "ng", "ҧ" to "p", "ҩ" to "o", "ҫ" to "s",
        "ҭ" to "t", "ү" to "u", "ұ" to "u", "ҳ" to "kh", "ҵ" to "ts",
        "ҷ" to "ch", "ҹ" to "ch", "һ" to "h", "ҽ" to "ch", "ҿ" to "ch",
        "ќ" to "ḱ", "ө" to "ö"
    )

    private val RUSSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "ого" to "ovo", "Ого" to "Ovo", "его" to "evo", "Его" to "Evo"
    )

    private val UKRAINIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Г" to "H", "г" to "h",
        "Ґ" to "G", "ґ" to "g",
        "Є" to "Ye", "є" to "ye",
        "І" to "I", "і" to "i",
        "Ї" to "Yi", "ї" to "yi"
    )

    private val SERBIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ж" to "Ž", "Љ" to "Lj", "Њ" to "Nj", "Ц" to "C", "Ч" to "Č",
        "Џ" to "Dž", "Ш" to "Š", "Х" to "H",

        "ж" to "ž", "љ" to "lj", "њ" to "nj", "ц" to "c", "ч" to "č",
        "џ" to "dž", "ш" to "š", "х" to "h"
    )

    private val BULGARIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ж" to "Zh", "Ц" to "Ts", "Ч" to "Ch", "Ш" to "Sh", "Щ" to "Sht",
        "Ъ" to "A", "Ь" to "Y", "Ю" to "Yu", "Я" to "Ya",

        "ж" to "zh", "ц" to "ts", "ч" to "ch", "ш" to "sh", "щ" to "sht",
        "ъ" to "a", "ь" to "y", "ю" to "yu", "я" to "ya"
    )

    private val BELARUSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Г" to "H", "г" to "h", "Ў" to "W", "ў" to "w"
    )

    private val KYRGYZ_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ү" to "Ü", "ү" to "ü", "Ы" to "Y", "ы" to "y"
    )

    private val MACEDONIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ѓ" to "Gj", "Ѕ" to "Dz", "И" to "I", "Ј" to "J", "Љ" to "Lj",
        "Њ" to "Nj", "Ќ" to "Kj", "Џ" to "Dž", "Ч" to "Č", "Ш" to "Sh",
        "Ж" to "Zh", "Ц" to "C", "Х" to "H",

        "ѓ" to "gj", "ѕ" to "dz", "и" to "i", "ј" to "j", "љ" to "lj",
        "њ" to "nj", "ќ" to "kj", "џ" to "dž", "ч" to "č", "ш" to "sh",
        "ж" to "zh", "ц" to "c", "х" to "h"
    )

    private val RUSSIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М", "Н",
        "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы", "Ь",
        "Э", "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к", "л", "м", "н",
        "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ", "ъ", "ы", "ь",
        "э", "ю", "я"
    )

    private val UKRAINIAN_CYRILLIC_LETTERS = setOf(
       "А", "Б", "В", "Г", "Ґ", "Д", "Е", "Є", "Ж", "З", "И", "І", "Ї", "Й",
        "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч",
        "Ш", "Щ", "Ь", "Ю", "Я",

        "а", "б", "в", "г", "ґ", "д", "е", "є", "ж", "з", "и", "і", "ї", "й",
        "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч",
        "ш", "щ", "ь", "ю", "я"
    )

    private val SERBIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Ђ", "Е", "Ж", "З", "И", "Ј", "К", "Л", "Љ", "М",
        "Н", "Њ", "О", "П", "Р", "С", "Т", "Ћ", "У", "Ф", "Х", "Ц", "Ч", "Џ", "Ш",

        "а", "б", "в", "г", "д", "ђ", "е", "ж", "з", "и", "ј", "к", "л", "љ", "м",
        "н", "њ", "о", "п", "р", "с", "т", "ћ", "у", "ф", "х", "ц", "ч", "џ", "ш"
    )

    private val BULGARIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "И", "Й", "К", "Л", "М",
        "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ",
        "Ъ", "Ь", "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ж", "з", "и", "й", "к", "л", "м",
        "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ",
        "ъ", "ь", "ю", "я"
    )

    private val BELARUSIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "І", "Й", "К", "Л", "М", "Н",
        "О", "П", "Р", "С", "Т", "У", "Ў", "Ф", "Х", "Ц", "Ч", "Ш", "Ь", "Ю", "Я",
        "Ы", "Э",

        "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "і", "й", "к", "л", "м", "н",
        "о", "п", "р", "с", "т", "у", "ў", "ф", "х", "ц", "ч", "ш", "ь", "ю", "я",
        "ы", "э"
    )

    private val KYRGYZ_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М", "Н",
        "Ң", "О", "Ө", "П", "Р", "С", "Т", "У", "Ү", "Ф", "Х", "Ц", "Ч", "Ш", "Щ",
        "Ъ", "Ы", "Ь", "Э", "Ю", "Я",

        "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к", "л", "м", "н",
        "ң", "о", "ө", "п", "р", "с", "т", "у", "ү", "ф", "х", "ц", "ч", "ш", "щ",
        "ъ", "ы", "ь", "э", "ю", "я"
    )

    private val MACEDONIAN_CYRILLIC_LETTERS = setOf(
        "А", "Б", "В", "Г", "Д", "Ѓ", "Е", "Ж", "З", "Ѕ", "И", "Ј", "К", "Л",
        "Љ", "М", "Н", "Њ", "О", "П", "Р", "С", "Т", "Ќ", "У", "Ф", "Х",
        "Ц", "Ч", "Џ", "Ш",

        "а", "б", "в", "г", "д", "ѓ", "е", "ж", "з", "ѕ", "и", "ј", "к", "л",
        "љ", "м", "н", "њ", "о", "п", "р", "с", "т", "ќ", "у", "ф", "х",
        "ц", "ч", "џ", "ш"
    )

    private val UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ґ", "ґ", "Є", "є", "І", "і", "Ї", "ї"
    )

    private val SERBIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ђ", "ђ", "Ј", "ј", "Љ", "љ", "Њ", "њ", "Ћ", "ћ", "Џ", "џ"
    )

    private val BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ў", "ў", "І", "і"
    )

    private val KYRGYZ_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ң", "ң", "Ө", "ө", "Ү", "ү"
    )

    private val MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ѓ", "ѓ", "Ѕ", "ѕ", "Ќ", "ќ"
    )

    private val kuromojiTokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    /**
     * Writing systems we care about telling apart when deciding whether a provider handed back
     * the song or handed back a translation of it.
     */
    enum class Script { LATIN, HAN, KANA, HANGUL, CYRILLIC, ARABIC, DEVANAGARI, THAI, GREEK, HEBREW, UNKNOWN }

    private fun scriptOf(ch: Char): Script? {
        if (!ch.isLetter()) return null
        return when (ch.code) {
            in 0x0041..0x024F -> Script.LATIN
            in 0x0370..0x03FF -> Script.GREEK
            in 0x0400..0x052F -> Script.CYRILLIC
            in 0x0590..0x05FF -> Script.HEBREW
            in 0x0600..0x06FF, in 0x0750..0x077F -> Script.ARABIC
            in 0x0900..0x097F -> Script.DEVANAGARI
            in 0x0E00..0x0E7F -> Script.THAI
            in 0x3040..0x30FF -> Script.KANA
            in 0xAC00..0xD7AF, in 0x1100..0x11FF -> Script.HANGUL
            in 0x3400..0x4DBF, in 0x4E00..0x9FFF, in 0xF900..0xFAFF -> Script.HAN
            else -> null
        }
    }

    /**
     * The script most of [text] is written in.
     *
     * Kana wins over Han whenever any kana is present: Japanese is mostly kanji by character count
     * but the presence of kana is decisive, and calling a Japanese song "Chinese" sends it to the
     * pinyin romanizer, which is exactly the "why is this in Chinese" symptom.
     */
    fun dominantScript(text: String): Script {
        val counts = HashMap<Script, Int>()
        var total = 0
        for (ch in text) {
            val s = scriptOf(ch) ?: continue
            counts[s] = (counts[s] ?: 0) + 1
            total++
        }
        if (total == 0) return Script.UNKNOWN
        if ((counts[Script.KANA] ?: 0) > 0) return Script.KANA
        if ((counts[Script.HANGUL] ?: 0) * 4 >= total) return Script.HANGUL
        return counts.maxByOrNull { it.value }?.key ?: Script.UNKNOWN
    }

    // the script the readings are for: kana settles it; otherwise the commonest script that isn't latin, as long as a tenth of the letters are in it, so a korean song with english verses still reads as korean instead of losing its readings
    fun romanizableScript(text: String): Script {
        val counts = HashMap<Script, Int>()
        var total = 0
        for (ch in text) {
            val s = scriptOf(ch) ?: continue
            counts[s] = (counts[s] ?: 0) + 1
            total++
        }
        if (total == 0) return Script.UNKNOWN
        if ((counts[Script.KANA] ?: 0) > 0) return Script.KANA
        val (script, count) = counts.filterKeys { it != Script.LATIN }.maxByOrNull { it.value } ?: return Script.UNKNOWN
        return if (count * 10 >= total) script else Script.UNKNOWN
    }


    suspend fun romanizeJapanese(text: String): String = withContext(Dispatchers.Default) {
        val tokens = kuromojiTokenizer.tokenize(text)
        val romanizedTokens = tokens.mapIndexed { index, token ->
            val currentReading = if (token.reading.isNullOrEmpty() || token.reading == "*") {
                token.surface
            } else {
                token.reading
            }
            val nextTokenReading = if (index + 1 < tokens.size) {
                tokens[index + 1].reading?.takeIf { it.isNotEmpty() && it != "*" } ?: tokens[index + 1].surface
            } else {
                null
            }
            katakanaToRomaji(currentReading, nextTokenReading)
        }
        romanizedTokens.joinToString(" ")
    }

    // a word's katakana reading in romaji; the next word's reading is only for a small tsu that ends this one
    fun katakanaToRomaji(katakana: String?, nextKatakana: String? = null): String {
        if (katakana.isNullOrEmpty()) return ""

        val romaji = StringBuilder(katakana.length * 2)
        var i = 0
        while (i < katakana.length) {
            val ch = katakana[i]
            when (ch) {
                // a small tsu doubles the sound right after it, so ハッピー is happii, not the next word's letter
                'ッ' -> {
                    val after = if (i + 1 < katakana.length) katakana.substring(i + 1) else nextKatakana
                    doubledConsonant(after)?.let { romaji.append(it) }
                    i++
                }
                // a long-vowel mark stretches the vowel before it, so デザート reads dezaato instead of dezato
                'ー' -> {
                    romaji.lastOrNull()?.takeIf { it in "aiueo" }?.let { romaji.append(it) }
                    i++
                }
                else -> {
                    val pair = if (i + 1 < katakana.length) KANA_ROMAJI_MAP[katakana.substring(i, i + 2)] else null
                    if (pair != null) {
                        romaji.append(pair)
                        i += 2
                    } else {
                        romaji.append(KANA_ROMAJI_MAP[ch.toString()] ?: ch.toString())
                        i++
                    }
                }
            }
        }
        return romaji.toString().lowercase()
    }

    // the consonant a small tsu doubles before kana, or null when there's no consonant to double
    private fun doubledConsonant(kana: String?): Char? {
        if (kana.isNullOrEmpty()) return null
        val sound = (if (kana.length >= 2) KANA_ROMAJI_MAP[kana.substring(0, 2)] else null)
            ?: KANA_ROMAJI_MAP[kana.substring(0, 1)]
            ?: return null
        return when {
            sound.startsWith("ch") -> 't'
            sound.first() in "aiueon" -> null
            else -> sound.first()
        }
    }

    suspend fun romanizeKorean(text: String): String = withContext(Dispatchers.Default) {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null

        for (i in text.indices) {
            val char = text[i]
            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey)
                        ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal)
                        ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)
                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }

        if (prevFinal != null) {
            val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
            romajaBuilder.append(jong)
        }

        romajaBuilder.toString()
    }

    suspend fun romanizeChinese(text: String): String = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext ""
        val builder = StringBuilder(text.length * 2)
        for (ch in text) {
            if (ch in '\u4E00'..'\u9FFF') {
                val py = Pinyin.toPinyin(ch).lowercase(Locale.getDefault())
                builder.append(py).append(' ')
            } else {
                builder.append(ch)
            }
        }

        builder.toString()
            .replace(Regex("\\s+([,.!?;:])"), "$1")
            .replace(Regex("\\s+([，。！？；：、（）《》〈〉【】『』「」])"), "$1")
            .trim()
    }

    suspend fun romanizeCyrillic(text: String): String? = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext null

        val cyrillicChars = text.filter { it in '\u0400'..'\u04FF' }

        if (cyrillicChars.isEmpty() ||
            (cyrillicChars.length == 1 && (cyrillicChars[0] == 'е' || cyrillicChars[0] == 'Е'))) {
            return@withContext null
        }

        when {
            isRussian(text) -> romanizeRussianInternal(text)
            isUkrainian(text) -> romanizeUkrainianInternal(text)
            isSerbian(text) -> romanizeSerbianInternal(text)
            isBulgarian(text) -> romanizeBulgarianInternal(text)
            isBelarusian(text) -> romanizeBelarusianInternal(text)
            isKyrgyz(text) -> romanizeKyrgyzInternal(text)
            isMacedonian(text) -> romanizeMacedonianInternal(text)
            else -> null
        }
    }

    private fun romanizeRussianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    var consumed = false

                    if (charIndex + 2 < word.length) {
                        val threeCharCandidate = word.substring(charIndex, charIndex + 3)
                        if (RUSSIAN_ROMAJI_MAP.containsKey(threeCharCandidate)) {
                            romajiBuilder.append(RUSSIAN_ROMAJI_MAP[threeCharCandidate])
                            charIndex += 3
                            consumed = true
                        }
                    }

                    if (!consumed) {
                        val charStr = word[charIndex].toString()

                        if ((charStr == "е" || charStr == "Е") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                            romajiBuilder.append(if (charStr == "е") "ye" else "Ye")
                        } else {

                            val romanizedChar = GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                            romajiBuilder.append(romanizedChar)
                        }
                        charIndex += 1
                    }
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeUkrainianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    var processed = false

                    if (charIndex > 0 && word[charIndex - 1].isLetter() && !isCyrillicVowel(word[charIndex - 1])) {

                        if (charStr == "Ю") {
                            romajiBuilder.append("Iu")
                            processed = true
                        } else if (charStr == "ю") {
                            romajiBuilder.append("iu")
                            processed = true
                        } else if (charStr == "Я") {
                            romajiBuilder.append("Ia")
                            processed = true
                        } else if (charStr == "я") {
                            romajiBuilder.append("ia")
                            processed = true
                        }
                    }

                    if (!processed) {
                        romajiBuilder.append(UKRAINIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    }
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeSerbianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = SERBIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeBulgarianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = BULGARIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeBelarusianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEach { word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()

                    if ((charStr == "е" || charStr == "Е") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                        romajiBuilder.append(if (charStr == "е") "ye" else "Ye")
                    } else {

                        val romanizedChar = BELARUSIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                        romajiBuilder.append(romanizedChar)
                    }
                    charIndex += 1
                }
            }
        }

        return romajiBuilder.toString()
    }

    private fun romanizeKyrgyzInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = KYRGYZ_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeMacedonianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = MACEDONIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    fun isRussian(text: String): Boolean {
        return text.any { char ->
            RUSSIAN_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            val charStr = char.toString()
            RUSSIAN_CYRILLIC_LETTERS.contains(charStr) || !charStr.matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isUkrainian(text: String): Boolean {
        return text.any { char ->
            UKRAINIAN_CYRILLIC_LETTERS.contains(char.toString()) || UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            UKRAINIAN_CYRILLIC_LETTERS.contains(char.toString()) || UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isSerbian(text: String): Boolean {
        return text.any { char ->
            SERBIAN_CYRILLIC_LETTERS.contains(char.toString()) || SERBIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            SERBIAN_CYRILLIC_LETTERS.contains(char.toString()) || SERBIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isBulgarian(text: String): Boolean {
        return text.any { char ->
            BULGARIAN_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            BULGARIAN_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isBelarusian(text: String): Boolean {
        return text.any { char ->
            BELARUSIAN_CYRILLIC_LETTERS.contains(char.toString()) || BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            BELARUSIAN_CYRILLIC_LETTERS.contains(char.toString()) || BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isKyrgyz(text: String): Boolean {
        return text.any { char ->
            KYRGYZ_CYRILLIC_LETTERS.contains(char.toString()) || KYRGYZ_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            KYRGYZ_CYRILLIC_LETTERS.contains(char.toString()) || KYRGYZ_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isMacedonian(text: String): Boolean {
        return text.any { char ->
            MACEDONIAN_CYRILLIC_LETTERS.contains(char.toString()) || MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            MACEDONIAN_CYRILLIC_LETTERS.contains(char.toString()) || MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') ||
                    (char in '\u30A0'..'\u30FF') ||
                    (char in '\u4E00'..'\u9FFF')
        }
    }

    fun isKorean(text: String): Boolean {
        return text.any { char ->
            (char in '\uAC00'..'\uD7A3')
        }
    }

    fun isChinese(text: String): Boolean {
        if (text.isEmpty()) return false
        val cjkCharCount = text.count { char -> char in '\u4E00'..'\u9FFF' }
        val hiraganaKatakanaCount = text.count { char -> (char in '\u3040'..'\u309F') || (char in '\u30A0'..'\u30FF') }
        return cjkCharCount > 0 && (hiraganaKatakanaCount.toDouble() / text.length.toDouble()) < 0.1
    }

    fun isHindi(text: String): Boolean {
        return text.any { char ->
            char in '\u0900'..'\u097F'
        }
    }

    suspend fun romanizeHindi(text: String): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            var consumed = false

            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = DEVANAGARI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val charStr = text[i].toString()
                sb.append(DEVANAGARI_ROMAJI_MAP[charStr] ?: charStr)
                i += 1
            }
        }
        sb.toString()
    }

    fun isPunjabi(text: String): Boolean {
        return text.any { char ->
            char in '\u0A00'..'\u0A7F'
        }
    }

    suspend fun romanizePunjabi(text: String): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val char = text[i]
            var consumed = false

            if (char == '\u0A71') {

                 if (i + 1 < text.length) {
                     val nextCharStr = text[i+1].toString()
                     val nextMapped = GURMUKHI_ROMAJI_MAP[nextCharStr]
                     if (nextMapped != null && nextMapped.isNotEmpty()) {
                         sb.append(nextMapped[0])
                     }
                 }
                 i++
                 continue
            }

            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = GURMUKHI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val str = char.toString()
                sb.append(GURMUKHI_ROMAJI_MAP[str] ?: str)
                i++
            }
        }
        sb.toString()
    }

    private fun isCyrillicVowel(char: Char): Boolean {
        return "АаЕеЄєИиІіЇїОоУуЮюЯяЫыЭэ".contains(char)
    }

    /** the reading of one lyric line, or null when it's in the Latin alphabet already (or nothing could be read) */
    suspend fun read(text: String): String? {
        if (text.isBlank()) return null
        val out = try {
            when {
                isPunjabi(text) -> romanizePunjabi(text)
                else -> when (romanizableScript(text)) {
                    Script.KANA -> romanizeJapanese(text)
                    Script.HAN -> if (isChinese(text)) romanizeChinese(text) else romanizeJapanese(text)
                    Script.HANGUL -> romanizeKorean(text)
                    Script.CYRILLIC -> romanizeCyrillic(text)
                    Script.DEVANAGARI -> romanizeHindi(text)
                    else -> null
                }
            }
        } catch (_: Throwable) { null }
        return out?.trim()?.takeIf { it.isNotEmpty() && !it.equals(text.trim(), ignoreCase = true) }
    }

    /** one reading per line (null where there's none); the whole song is read in the background */
    suspend fun readAll(lines: List<String>): List<String?> = withContext(Dispatchers.Default) { lines.map { read(it) } }
}
