package com.jetbrains.edu.learning.courseFormat

import com.intellij.lang.Language
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.RoundedBorderWithPadding
import com.jetbrains.edu.learning.languageColors.ProgrammingLanguageColorManager
import java.awt.Color
import java.util.*
import javax.swing.JComponent

private val DEFAULT_COLOR: Color = Color(70, 130, 180, 70)

open class Tag @JvmOverloads constructor(val text: String, val color: Color = DEFAULT_COLOR, private val searchOption: String = "tag") {
  fun getSearchText() : String = "$searchOption:$text".toLowerCase()

  fun accept(filter: String): Boolean {
    val textInLowerCase = text.toLowerCase(Locale.getDefault())
    if (textInLowerCase.contains(filter)) {
      return true
    }
    val searchPrefix = "$searchOption:"
    return filter.startsWith(searchPrefix) && textInLowerCase.contains(filter.substring(searchPrefix.length))
  }

    fun createComponent(isSelected: Boolean = false): JComponent {
        val label = JBLabel(text)
        label.border = RoundedBorderWithPadding(JBUI.scale(20), isSelected, color, UIUtil.getPanelBackground())
        return label
    }
}

class ProgrammingLanguageTag(language: Language) :
        Tag(language.displayName, ProgrammingLanguageColorManager[language] ?: DEFAULT_COLOR, "programming_language")

class HumanLanguageTag(languageName: String): Tag(languageName, searchOption = "language")

class FeaturedTag: Tag("Featured", Color(151, 118, 169))

class InProgressTag: Tag("In Progress", Color(255,255,224))