package com.openclaw.assistant.speech.voicevox

/**
 * VOICEVOX character information including credit notation.
 *
 * styleId values match SettingsActivity.VoiceVoxCharacters.CHARACTERS and the
 * VoiceVox synthesizer API — do NOT change them independently.
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
 */
object VoiceVoxCharacters {

    private val characters = listOf(
        // ── 0.vvm ── 四国めたん
        VoiceVoxCharacter(0, "四国めたん", "あまあま", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(1, "四国めたん", "ノーマル", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(2, "四国めたん", "セクシー", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(3, "四国めたん", "ツンツン", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(4, "四国めたん", "ささやき", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),

        // ── 0.vvm ── ずんだもん
        VoiceVoxCharacter(6, "ずんだもん", "あまあま", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(7, "ずんだもん", "ノーマル", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(8, "ずんだもん", "セクシー", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(9, "ずんだもん", "ツンツン", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(10, "ずんだもん", "ささやき", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(11, "ずんだもん", "ヒソヒソ", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),

        // ── 0.vvm ── 春日部つむぎ
        VoiceVoxCharacter(12, "春日部つむぎ", "ノーマル", "0",
            "VOICEVOX:春日部つむぎ", "© 春日部つむぎ",
            "https://tsumugi-official.studio.site/rule"),

        // ── 1.vvm ── 雨晴はう
        VoiceVoxCharacter(14, "雨晴はう", "ノーマル", "1",
            "VOICEVOX:雨晴はう", "© 雨晴はう",
            "https://amehau.com/rules/amehare-hau-rule"),

        // ── 2.vvm ── 波音リツ
        VoiceVoxCharacter(15, "波音リツ", "ノーマル", "2",
            "VOICEVOX:波音リツ", "© カノンの落ちる城",
            "https://www.canon-voice.com/"),

        // ── 2.vvm ── 玄野武宏
        VoiceVoxCharacter(16, "玄野武宏", "ノーマル", "2",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(17, "玄野武宏", "喜び", "2",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(18, "玄野武宏", "ツンギレ", "2",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(19, "玄野武宏", "悲しみ", "2",
            "VOICEVOX:玄野武宏", "© 玄野武宏 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),

        // ── 3.vvm ── 白上虎太郎
        VoiceVoxCharacter(20, "白上虎太郎", "ふつう", "3",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(21, "白上虎太郎", "わーい", "3",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(22, "白上虎太郎", "びくびく", "3",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(23, "白上虎太郎", "おこ", "3",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),
        VoiceVoxCharacter(24, "白上虎太郎", "びえーん", "3",
            "VOICEVOX:白上虎太郎", "© 白上虎太郎",
            "https://frontier.creatia.cc/fandoms/portal/creations/4"),

        // ── 4.vvm ── 青山龍星
        VoiceVoxCharacter(27, "青山龍星", "ノーマル", "4",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(28, "青山龍星", "熱血", "4",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(29, "青山龍星", "不機嫌", "4",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(30, "青山龍星", "喜び", "4",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(31, "青山龍星", "悲しみ", "4",
            "VOICEVOX:青山龍星", "© 青山龍星 (ViRVOX Project)",
            "https://virvoxproject.wixsite.com/official/voicevox%E5%88%A9%E7%94%A8%E8%A6%8F%E7%B4%84"),
        VoiceVoxCharacter(32, "青山龍星", "囁き", "4",
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
