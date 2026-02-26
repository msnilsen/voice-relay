package com.openclaw.assistant.speech.voicevox

/**
 * VOICEVOX character information including credit notation
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
 * VOICEVOX character database
 */
object VoiceVoxCharacters {
    
    private val characters = listOf(
        // 0.vvm - Shikoku Metan
        VoiceVoxCharacter(0, "四国めたん", "あまあま", "0", 
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(2, "四国めたん", "ノーマル", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(4, "四国めたん", "セクシー", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(6, "四国めたん", "ツンツン", "0",
            "VOICEVOX:四国めたん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        
        // 0.vvm - Zundamon
        VoiceVoxCharacter(1, "ずんだもん", "あまあま", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(3, "ずんだもん", "ノーマル", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(5, "ずんだもん", "セクシー", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(7, "ずんだもん", "ツンツン", "0",
            "VOICEVOX:ずんだもん", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        
        // 0.vvm - Kasukabe Tsumugi
        VoiceVoxCharacter(8, "春日部つむぎ", "ノーマル", "0",
            "VOICEVOX:春日部つむぎ", "© 春日部つむぎ",
            "https://tsumugi-official.studio.site/rule"),
        
        // 0.vvm - Amehare Hau
        VoiceVoxCharacter(10, "雨晴はう", "ノーマル", "0",
            "VOICEVOX:雨晴はう", "© 雨晴はう",
            "https://amehau.com/rules/amehare-hau-rule"),
        
        // 1.vvm - Meimei Himari
        VoiceVoxCharacter(14, "冥鳴ひまり", "ノーマル", "1",
            "VOICEVOX:冥鳴ひまり", "© 冥鳴ひまり",
            "https://www.meimeihimari.com/terms-of-use"),
        
        // 2.vvm - Kyushu Sora
        VoiceVoxCharacter(15, "九州そら", "あまあま", "2",
            "VOICEVOX:九州そら", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(16, "九州そら", "ノーマル", "2",
            "VOICEVOX:九州そら", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(17, "九州そら", "セクシー", "2",
            "VOICEVOX:九州そら", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        VoiceVoxCharacter(18, "九州そら", "ツンツン", "2",
            "VOICEVOX:九州そら", "© 東北ずん子プロジェクト",
            "https://zunko.jp/con_ongen_kiyaku.html"),
        
        // 3.vvm - Namine Ritsu
        VoiceVoxCharacter(9, "波音リツ", "ノーマル", "3",
            "VOICEVOX:波音リツ", "© カノンの落ちる城",
            "https://www.canon-voice.com/"),
        
        // 15.vvm - Mochiko-san (CV credit required)
        VoiceVoxCharacter(20, "もち子さん", "ノーマル", "15",
            "VOICEVOX:もち子(cv 明日葉よもぎ)", "© もちぞら模型店",
            "https://vtubermochio.wixsite.com/mochizora/利用規約",
            requiresCvCredit = true),
        
        // Add more characters as needed...
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
