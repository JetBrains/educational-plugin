package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYCHARM
import com.jetbrains.edu.learning.courseFormat.Language
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.yaml.InjectableValueKey
import com.jetbrains.edu.learning.yaml.format.CourseBuilder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDU_YAML_TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MARKETPLACE_YAML_TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.getEduValue

/**
 * YAML version is added to an [ObjectMapper] after it has migrated a course.
 * The information about the YAML version of the course is then used to migrate all other study items.
 */
typealias AdditionalFilesCollector = (courseType: String, environment: String, languageId: String) -> List<EduFile>
val ADDITIONAL_FILES_COLLECTOR_MAPPER_KEY = InjectableValueKey<AdditionalFilesCollector>("additional_files_searcher")

/**
 * Collect all additional files and write them to YAML.
 * In the intelli-plugin, additional files are collected with `EduConfigurator`.
 * To get a configurator for the course, we need the course type, environment and language id.
 */
object ToVersion2StepAddAdditionalFiles : YamlMigrationStep {

  override fun migrateCourse(mapper: ObjectMapper, config: ObjectNode): ObjectNode {
    val additionalFilesCollector = mapper.getEduValue(ADDITIONAL_FILES_COLLECTOR_MAPPER_KEY)
    if (additionalFilesCollector == null) {
      logger<ToVersion2StepAddAdditionalFiles>().severe("No additional files collector provided")
      return config
    }

    // get the course type, environment and language id
    val rawType = config.get(TYPE)?.asText()
    val rawEnvironment = config.get(ENVIRONMENT)?.asText()
    val rawLanguageId = config.get(PROGRAMMING_LANGUAGE)?.asText()

    if (rawLanguageId == null) {
      logger<ToVersion2StepAddAdditionalFiles>().severe("Failed to read language ID from course-info.yaml")
      return config
    }

    val type = decodeCourseType(rawType)
    val environment = rawEnvironment ?: DEFAULT_ENVIRONMENT
    val languageId = Language.findLanguageByName(rawLanguageId)

    if (languageId == null) {
      logger<ToVersion2StepAddAdditionalFiles>().severe("Failed to find language with ID `$rawLanguageId`")
      return config
    }

    val additionalFiles = additionalFilesCollector(type, environment, languageId)
    val additionalFilesList = mapper.valueToTree<ArrayNode>(additionalFiles)

    return config.set(ADDITIONAL_FILES, additionalFilesList)
  }

  /**
   * The logic is copied from [CourseBuilder.makeCourse] and methods that override it.
   * The real course type could also sometimes be MARKETPLACE, but we are going to use the return value
   * only for calling method [EduConfiguratorManager.findConfigurator] and it treats the type MARKETPLACE the same as the type PYCHARM.
   */
  private fun decodeCourseType(rawType: String?): String =
    when (rawType) {
      EDU_YAML_TYPE, MARKETPLACE_YAML_TYPE, null -> PYCHARM
      else -> rawType.replaceFirstChar { it.uppercaseChar() }
    }

}