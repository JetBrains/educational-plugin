package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

interface CodeforcesLanguageProvider {
  val codeforcesLanguageNamings: List<String>
  val configurator: EduConfigurator<*>?
    get() {
      return EduConfiguratorManager.allExtensions().find { it.language == languageId }?.instance
    }
  val languageId: String
  val preferableCodeforcesLanguage: String
    get() = codeforcesLanguageNamings.first()
  val templateFileName: String

  fun getLanguageVersion(codeforcesLanguage: String): String? = null

  fun createTaskFiles(task: Task): List<TaskFile> {
    val text = GeneratorUtils.getInternalTemplateText(templateFileName)
    val fileName = templateFileName.removePrefix("codeforces.")
    return listOf(TaskFile(GeneratorUtils.joinPaths(task.sourceDir, fileName), text))
  }

  companion object {
    val EP_NAME: ExtensionPointName<CodeforcesLanguageProvider> =
      ExtensionPointName.create("Educational.codeforcesLanguageProvider")

    fun getSupportedLanguages(): List<String> {
      return EP_NAME.extensions.flatMap { it.codeforcesLanguageNamings }
    }

    fun getPreferableCodeforcesLanguage(languageId: String): String? {
      EP_NAME.extensions.forEach {
        if (languageId == it.languageId) return it.preferableCodeforcesLanguage
      }
      return null
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

    fun generateTaskFiles(task: Task): List<TaskFile>? =
      EP_NAME.extensions
        .firstOrNull { it.languageId == task.course.languageID }
        ?.createTaskFiles(task)

    fun getConfigurator(languageId: String): EduConfigurator<*>? {
      return EP_NAME.extensions.find { it.languageId == languageId }?.configurator
    }
  }
}