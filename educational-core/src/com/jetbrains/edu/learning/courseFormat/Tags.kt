package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import java.util.*

@NonNls
private const val PROGRAMMING_LANGUAGE_TAG_SEARCH_OPTION = "programming_language"
@NonNls
private const val LANGUAGE_TAG_SEARCH_OPTION = "language"

open class Tag @JvmOverloads constructor(val text: String, private val searchOption: String = "tag") {
  fun getSearchText(): String = "$searchOption:$text".lowercase()

  fun accept(filter: String): Boolean {
    val textInLowerCase = text.lowercase(Locale.getDefault())
    if (textInLowerCase.contains(filter)) {
      return true
    }
    val searchPrefix = "$searchOption:"
    return filter.startsWith(searchPrefix) && textInLowerCase.contains(filter.substring(searchPrefix.length))
  }
}

class ProgrammingLanguageTag(language: String) : Tag(language, PROGRAMMING_LANGUAGE_TAG_SEARCH_OPTION)

class HumanLanguageTag(languageName: String) : Tag(languageName, LANGUAGE_TAG_SEARCH_OPTION)

class FeaturedTag : Tag(EduCoreBundle.message("course.dialog.tags.featured"))

class InProgressTag : Tag(EduCoreBundle.message("course.dialog.tags.in.progress"))