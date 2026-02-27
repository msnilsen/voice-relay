package com.openclaw.assistant.speech.voicevox

/**
 * VOICEVOX character information including credit notation.
 *
 * styleId values match the official VOICEVOX Core API (voicevox_vvm 0.16.x).
 * Do NOT use arbitrary sequential IDs — always refer to the official speaker manifest.
 */
data class VoiceVoxCharacter(
    val styleId: Int,
    val name: String,
    val styleName: String,
    val vvmFile: String,
    val creditNotation: String,
    val copyright: String,
    val termsUrl: String,
    val requiresCvCredit: Boolean = false
) {
    override fun toString(): String = "$name（$styleName）"

    fun getFullCredit(): String = creditNotation
}

/**
 * VOICEVOX character database.
 *
 * Style IDs are authoritative and must stay in sync with:
 *   - SettingsActivity.VoiceVoxCharacters.CHARACTERS (UI / character picker)
 *   - SettingsActivity.VoiceVoxCharacters.VVM_FILE_MAPPING (download management)
 *   - VoiceVoxProvider.getVvmFileNameForStyle() (synthesis)
 *
 * Source of truth: voicevox_vvm README (https://github.com/VOICEVOX/voicevox_vvm)
 * VVM version: 0.16.x
 */
object VoiceVoxCharacters {

    private val characters = listOf(
        // ── 0.vvm ── 四国めたん
        VoiceVoxCharacter(0,  "四国めたん", "あまあま", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(2,  "四国めたん", "ノーマル",  "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(4,  "四国めたん", "セクシー", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(6,  "四国めたん", "ツンツン", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),

        // ── 0.vvm ── ずんだもん
        VoiceVoxCharacter(1,  "ずんだもん", "あまあま", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(3,  "ずんだもん", "ノーマル",  "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(5,  "ずんだもん", "セクシー", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(7,  "ずんだもん", "ツンツン", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),

        // ── 0.vvm ── 春日部つむぎ
        VoiceVoxCharacter(8,  "春日部つむぎ", "ノーマル", "0",
            "VOICEVOX:春日部つむぎ", "© 春日部つむぎ",
            "https://tsumugi-official.studio.site/rule"),

        // ── 0.vvm ── 雨晴はう
        VoiceVoxCharacter(10, "雨晴はう", "ノーマル", "0",
            "VOICEVOX:雨晴はう", "© 雨晴はう",
            "https://amehau.com/rules/amehare-hau-rule"),

        // ── 3.vvm ── 波音リツ
        VoiceVoxCharacter(9,  "波音リツ", "ノーマル", "3",
            "VOICEVOX:波音リツ", "© カノンの落ちる城",
            "https://www.canon-voice.com/"),

        // ── 4.vvm ── 玄野武宏
        VoiceVoxCharacter(11, "玄野武宏", "ノーマル",  "4",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),

        // ── 5.vvm ── 四国めたん ささやき系
        VoiceVoxCharacter(36, "四国めたん", "ささやき", "5",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(37, "四国めたん", "ヒソヒソ", "5",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),

        // ── 5.vvm ── ずんだもん ささやき系
        VoiceVoxCharacter(22, "ずんだもん", "ささやき", "5",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(38, "ずんだもん", "ヒソヒソ", "5",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),

        // ── 9.vvm ── 白上虎太郎
        VoiceVoxCharacter(12, "白上虎太郎", "ふつう",   "9",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(32, "白上虎太郎", "わーい",   "9",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(33, "白上虎太郎", "びくびく", "9",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(34, "白上虎太郎", "おこ",     "9",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(35, "白上虎太郎", "びえーん", "9",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),

        // ── 10.vvm ── 玄野武宏 追加スタイル
        VoiceVoxCharacter(39, "玄野武宏", "喜び",     "10",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(40, "玄野武宏", "ツンギレ", "10",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(41, "玄野武宏", "悲しみ",   "10",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),

        // ── 15.vvm ── 青山龍星
        VoiceVoxCharacter(13, "青山龍星", "ノーマル",  "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(81, "青山龍星", "熱血",     "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(82, "青山龍星", "不機嫌",   "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(83, "青山龍星", "喜び",     "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(84, "青山龍星", "しっとり", "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(85, "青山龍星", "かなしみ", "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(86, "青山龍星", "囁き",     "15",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84")
    )

    fun getAllCharacters(): List<VoiceVoxCharacter> = characters

    fun getCharacterByStyleId(styleId: Int): VoiceVoxCharacter? {
        return characters.find { it.styleId == styleId }
    }

    fun getCharactersByVvm(vvmFile: String): List<VoiceVoxCharacter> {
        return characters.filter { it.vvmFile == vvmFile }
    }

    /**
     * Get credit notations for all used characters
     */
    fun getCreditsForUsedCharacters(styleIds: List<Int>): List<VoiceVoxCredit> {
        return styleIds.mapNotNull { styleId ->
            getCharacterByStyleId(styleId)?.let { character ->
                VoiceVoxCredit(
                    characterName = character.name,
                    creditNotation = character.creditNotation,
                    copyright = character.copyright,
                    termsUrl = character.termsUrl
                )
            }
        }.distinctBy { it.creditNotation }
    }

    data class VoiceVoxCredit(
        val characterName: String,
        val creditNotation: String,
        val copyright: String,
        val termsUrl: String
    )
}
