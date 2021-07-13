package com.jetbrains.edu.learning.courseFormat

import com.intellij.ide.plugins.newui.TagComponent
import java.util.*
import javax.swing.JComponent

open class Tag @JvmOverloads constructor(val text: String, private val searchOption: String = "tag") {
  fun getSearchText(): String = "$searchOption:$text".toLowerCase()

  fun accept(filter: String): Boolean {
    val textInLowerCase = text.toLowerCase(Locale.getDefault())
    if (textInLowerCase.contains(filter)) {
      return true
    }
    val searchPrefix = "$searchOption:"
    return filter.startsWith(searchPrefix) && textInLowerCase.contains(filter.substring(searchPrefix.length))
  }

  fun createComponent(): JComponent = TagComponent(text)
}

class ProgrammingLanguageTag(language: String) : Tag(language, "programming_language")

class HumanLanguageTag(languageName: String) : Tag(languageName, searchOption = "language")

class FeaturedTag : Tag("Featured")

class InProgressTag : Tag("In Progress")