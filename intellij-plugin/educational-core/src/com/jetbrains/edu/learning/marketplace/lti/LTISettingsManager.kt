package com.jetbrains.edu.learning.marketplace.lti

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.jetbrains.edu.learning.courseDir
import java.io.IOException
import java.util.*

/**
 * Provides the access to LTI settings.
 * The settings are stored in the "lti.yaml" file in the course root.
 * YAML format is:
 * launches:
 *   - id: id-of-the-first-launch
 *     lms_description: Moodle 1 at some University
 *   - id: id-of-the-second-launch
 *     lms_description: Moodle 2 at another University
 */
@Service(Service.Level.PROJECT)
class LTISettingsManager(private val project: Project) {

  var ltiSettings: LTISettings? = null

  fun reloadSettingsFromFile() = ApplicationManager.getApplication().runReadAction {
    val ltiSettingsFile = project.courseDir.findChild(LTI_SETTING_FILE_NAME)
    ltiSettings = null

    if (ltiSettingsFile == null) {
      logger<LTISettingsManager>().info("$LTI_SETTINGS_RELOAD_MESSAGE file not found")
      return@runReadAction
    }

    try {
      val yaml = ltiSettingsFile.readText()
      ltiSettings = YAML_MAPPER.readValue(yaml, LTISettings::class.java)
      logger<LTISettingsManager>().info("$LTI_SETTINGS_RELOAD_MESSAGE successfully read")
    }
    catch (e: JsonProcessingException) {
      logger<LTISettingsManager>().warn("$LTI_SETTINGS_RELOAD_MESSAGE failed to parse LTI settings", e)
    }
    catch (e: IOException) {
      logger<LTISettingsManager>().error("$LTI_SETTINGS_RELOAD_MESSAGE failed ot read file contents", e)
    }
  }

  //TODO not used by now, but will be used to persist a new launch after the course is open from online
  fun addLaunch(ltiLaunch: LTILaunch) {
    val oldLaunches = ltiSettings?.launches ?: emptyList()

    val oldLaunch = oldLaunches.find { it.id == ltiLaunch.id }

    val newLaunches = if (oldLaunch == null) {
      oldLaunches + ltiLaunch
    }
    else {
      oldLaunches.map { if (it.id == ltiLaunch.id) ltiLaunch else it }
    }

    val newLtiSettings = LTISettings(newLaunches)
    ltiSettings = newLtiSettings
    writeSettingsToFile(newLtiSettings)
  }

  private fun writeSettingsToFile(newLtiSettings: LTISettings) = runInEdt {
    try {
      val settingsFile = project.courseDir.findOrCreateChildData(null, LTI_SETTING_FILE_NAME)
      settingsFile.writeText(YAML_MAPPER.writeValueAsString(newLtiSettings))
    }
    catch (e: IOException) {
      logger<LTISettingsManager>().error("Failed to write LTI settings", e)
    }
  }

  companion object {
    const val LTI_SETTING_FILE_NAME = "lti.yaml"
    const val LTI_SETTINGS_RELOAD_MESSAGE = "Loading LTI settings from file:"

    private val YAML_MAPPER = createYamlMapper()

    fun instance(project: Project) = project.service<LTISettingsManager>()

    private fun createYamlMapper(): ObjectMapper {
      val yamlFactory = YAMLFactory.builder()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
        .build()

      return JsonMapper.builder(yamlFactory)
        .addModule(kotlinModule())
        .addModule(JavaTimeModule())
        .defaultLocale(Locale.ENGLISH)
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .serializationInclusion(JsonInclude.Include.NON_EMPTY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .disable(MapperFeature.AUTO_DETECT_FIELDS, MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS)
        .build()
    }
  }
}