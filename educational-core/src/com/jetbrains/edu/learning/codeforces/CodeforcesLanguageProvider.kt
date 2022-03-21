package com.jetbrains.edu.learning.codeforces

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import java.io.IOException
import java.net.URL
import javax.swing.Icon

interface CodeforcesLanguageProvider {
  val codeforcesLanguageNamings: List<String>
    get() = codeforcesLanguages[languageId]?.map { it.key } ?: emptyList()
  val configurator: EduConfigurator<*>?
    get() {
      return EduConfiguratorManager.allExtensions().find { it.language == languageId }?.instance
    }
  val languageId: String
  val preferableCodeforcesLanguage: String
    get() = codeforcesLanguageNamings.first()
  val templateFileName: String
  val displayTemplateName: String
  val languageIcon: Icon

  fun getLanguageVersion(codeforcesLanguage: String): String? = codeforcesLanguages.asSequence()
    .fold(mutableMapOf<String, String?>())
    { acc, item -> acc.also { newMap -> newMap.putAll(item.value.map { it.key to it.value.version }) } }[codeforcesLanguage]

  fun createTaskFiles(task: Task): List<TaskFile> {
    val text = GeneratorUtils.getJ2eeTemplateText(templateFileName)
    return listOf(TaskFile(GeneratorUtils.joinPaths(task.sourceDir, displayTemplateName), text))
  }

  companion object {
    val EP_NAME: ExtensionPointName<CodeforcesLanguageProvider> =
      ExtensionPointName.create("Educational.codeforcesLanguageProvider")
    //FIXME: upload to repo and change link before merge
    private const val LANGUAGES_LINK = "https://raw.githubusercontent.com/DonHalkon/test/main/README.md"

    // Map<LanguageID, Map<LanguageIDWithVersion, CodeforcesLanguage>>
    private val codeforcesLanguages: Map<String, Map<String, CodeforcesLanguage>>
    private val mapper = ObjectMapper()
    private val typeRef = object : TypeReference<Map<String, List<CodeforcesLanguage>>>() {}
    private val LOG = logger<CodeforcesLanguageProvider>()

    init {
      codeforcesLanguages = getCodeforcesLanguages()
    }

    // Map<LanguageID, Map<LanguageIDWithVersion, CodeforcesLanguage>>
    private fun getCodeforcesLanguages(): Map<String, Map<String, CodeforcesLanguage>> {
      return try {
        val conn = URL(LANGUAGES_LINK).openConnection()
        val jsonText = conn.getInputStream().bufferedReader().readText()
        mapper.readValue(jsonText, typeRef).map { entry -> entry.key to entry.value.associateBy { it.name } }.toMap()
      }
      catch (e: IOException) {
        LOG.warn("Failed to get languages list from ${LANGUAGES_LINK}")
        emptyMap()
      }
    }

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
     * @see [com.jetbrains.edu.learning.courseFormat.Course.languageID]
     * @see [com.jetbrains.edu.learning.courseFormat.Course.languageVersion]
     */
    fun getLanguageIdAndVersion(codeforcesLanguage: String): String {
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
      error("Failed to get language and id from codeForces $codeforcesLanguage")
    }

    fun getProgramTypeId(codeforcesLanguage: String): String? = codeforcesLanguages.asSequence()
      .fold(mutableMapOf<String, String>())
      { acc, item -> acc.also { newMap -> newMap.putAll(item.value.map { it.key to it.value.programTypeId }) } }[codeforcesLanguage]

    fun generateTaskFiles(task: Task): List<TaskFile>? =
      EP_NAME.extensions
        .firstOrNull { it.languageId == task.course.languageID }
        ?.createTaskFiles(task)

    fun getConfigurator(languageId: String): EduConfigurator<*>? {
      return EP_NAME.extensions.find { it.languageId == languageId }?.configurator
    }
  }
}

private data class CodeforcesLanguage(@JsonProperty var name: String = "",
                                      @JsonProperty var programTypeId: String = "",
                                      @JsonProperty var version: String? = null)