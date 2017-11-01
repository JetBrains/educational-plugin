package com.jetbrains.edu.learning.languageColors

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.intellij.lang.Language
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.EduPluginConfiguratorManager
import java.awt.Color

object LanguageColorManager {

  private val languageColors: Map<String, LanguageColor>

  init {
    val jsonReader = javaClass.getResourceAsStream("/languageColors/colors.json").bufferedReader()
    languageColors = Gson()
            .fromJson<Map<String, LanguageColor>>(jsonReader, object : TypeToken<Map<String, LanguageColor>>(){}.type)
            .mapKeys { (k, _) -> k.toLowerCase() }
  }

  operator fun get(language: Language): Color? {
    val tagColor = EduPluginConfiguratorManager.forLanguage(language)?.languageTagColor()
    return if (tagColor != null) {
      tagColor
    } else {
      val color = languageColors[language.displayName.toLowerCase()]?.color ?: return null
      ColorUtil.toAlpha(ColorUtil.fromHex(color), 178)
    }
  }
}

internal data class LanguageColor(
        @SerializedName("color") val color: String?
)
