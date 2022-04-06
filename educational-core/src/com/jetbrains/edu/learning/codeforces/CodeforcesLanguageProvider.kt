package com.jetbrains.edu.learning.codeforces

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.LANGUAGES_LINK
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import java.net.URL
import javax.swing.Icon

abstract class CodeforcesLanguageProvider {
  protected abstract val configurator: EduConfigurator<*>
  protected abstract val languageId: String
  abstract val templateFileName: String
  abstract val displayTemplateName: String
  abstract val languageIcon: Icon

  private val codeforcesLanguages: List<CodeforcesLanguage> by lazy {
    downloadCodeforcesLanguages()
  }

  private fun downloadCodeforcesLanguages(): List<CodeforcesLanguage> {
    if (isUnitTestMode) return listOf(CodeforcesLanguage("TEXT", "-1"))
    return try {
      val mapper = ObjectMapper()
      val typeRef = object : TypeReference<Map<String, List<CodeforcesLanguage>>>() {}
      val jsonText = URL(LANGUAGES_LINK).readText()
      mapper.readValue(jsonText, typeRef).filter { it.key == languageId }.flatMap { it.value }
    }
    catch (e: Exception) {
      LOG.error("Failed to get languages list from ${LANGUAGES_LINK}")
      emptyList()
    }
  }

  private fun getLanguageVersion(languageIdWithVersion: String): String? =
    codeforcesLanguages.firstOrNull { it.languageIdWithVersion == languageIdWithVersion }?.version

  private fun getProgramTypeId(languageIdWithVersion: String): String? =
    codeforcesLanguages.firstOrNull { it.languageIdWithVersion == languageIdWithVersion }?.programTypeId

  open fun createTaskFiles(task: Task): List<TaskFile> {
    val text = GeneratorUtils.getJ2eeTemplateText(templateFileName)
    return listOf(TaskFile(GeneratorUtils.joinPaths(task.sourceDir, displayTemplateName), text))
  }

  companion object {
    val EP_NAME: ExtensionPointName<CodeforcesLanguageProvider> =
      ExtensionPointName.create("Educational.codeforcesLanguageProvider")

    private val LOG = logger<CodeforcesLanguageProvider>()

    fun getSupportedLanguages(): List<String> {
      return EP_NAME.extensions.flatMap { languageProvider -> languageProvider.codeforcesLanguages.map { it.languageIdWithVersion } }
    }

    fun getPreferableCodeforcesLanguage(languageId: String): String? =
      EP_NAME.extensions.filter { it.languageId == languageId }.map { it.languageId }.firstOrNull()

    /**
     * @return Proper language with languageVersion splitted with space from codeforces programming language.
     * If languageVersion isn't specified - then only language is returned in result
     * @see [com.jetbrains.edu.learning.courseFormat.Course.languageID]
     * @see [com.jetbrains.edu.learning.courseFormat.Course.languageVersion]
     */
    fun getLanguageIdAndVersion(codeforcesLanguage: String): String {
      EP_NAME.extensions.forEach { languageProvider ->
        if (codeforcesLanguage in languageProvider.codeforcesLanguages.map { it.languageIdWithVersion }) {
          val languageId = languageProvider.languageId
          val languageVersion = languageProvider.getLanguageVersion(codeforcesLanguage)

          return if (languageVersion != null) {
            "$languageId $languageVersion"
          }
          else languageId
        }
      }
      error("Failed to get language and id from Codeforces $codeforcesLanguage")
    }

    fun getProgramTypeId(codeforcesLanguage: String): String {
      EP_NAME.extensions.forEach {
        val programTypeId = it.getProgramTypeId(codeforcesLanguage)
        if (programTypeId != null) {
          return programTypeId
        }
      }
      error("Failed to get ProgramTypeId for '$codeforcesLanguage'")
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

private class CodeforcesLanguage(
  @JsonProperty var languageIdWithVersion: String = "",
  @JsonProperty var programTypeId: String = "",
  @JsonProperty var version: String? = null
)
