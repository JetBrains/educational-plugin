package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.extensions.ExtensionPointName

interface CodeforcesLanguageProvider {
  val codeforcesLanguageNamings: List<String>
  val languageId: String

  fun getLanguageVersion(codeforcesLanguage: String): String? = null

  companion object {
    private val EP_NAME: ExtensionPointName<CodeforcesLanguageProvider> =
      ExtensionPointName.create("Educational.codeforcesLanguageProvider")

    fun getSupportedLanguages(): List<String> {
      val languages = mutableListOf<String>()
      EP_NAME.extensions.forEach {
        languages.addAll(it.codeforcesLanguageNamings)
      }
      return languages
    }

    /**
     * @return Proper language with languageVersion splitted with space from codeforces programming language.
     * If languageVersion isn't specified - then only language is returned in result
     * @see [com.jetbrains.edu.learning.courseFormat.Course.getLanguageID]
     * @see [com.jetbrains.edu.learning.courseFormat.Course.getLanguageVersion]
     */
    fun getLanguageIdAndVersion(codeforcesLanguage: String): String? {
      EP_NAME.extensions.forEach {
        if (codeforcesLanguage in it.codeforcesLanguageNamings) {
          val languageId = it.languageId
          val languageVersion = it.getLanguageVersion(codeforcesLanguage)

          return if (languageVersion != null) {
            "$languageId $languageVersion"
          }
          else languageId
        }
      }
      return null
    }
  }
}