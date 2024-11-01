package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.yaml.InjectableValueKey
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION
import com.jetbrains.edu.learning.yaml.getEduValue
import com.jetbrains.edu.learning.yaml.setEduValue

/**
 * YAML version is added to an [ObjectMapper] after it has migrated a course.
 * The information about the YAML version of the course is then used to migrate all other study items.
 */
val YAML_VERSION_MAPPER_KEY = InjectableValueKey<Int>("yaml_version")

class YamlMigrator(private val mapper: ObjectMapper) {

  private val migrationSteps = mapOf(
    1 to ToVersion1Step,
  )

  private fun getInitialYamlVersion(): Int? = mapper.getEduValue(YAML_VERSION_MAPPER_KEY)

  fun needMigration(): Boolean {
    val yamlVersion = getInitialYamlVersion()
    return yamlVersion != null && yamlVersion < CURRENT_YAML_VERSION
  }

  /**
   * Migrates tree of the course config, populates the mapper with the original YAML version
   */
  fun migrateCourse(configTree: ObjectNode): ObjectNode {
    val yamlVersion = configTree.get(YAML_VERSION)?.asInt(0) ?: 0
    mapper.setEduValue(YAML_VERSION_MAPPER_KEY, yamlVersion)

    return runMigrationSteps(configTree) {
      migrateCourse(mapper, it)
    }
  }

  fun migrateSection(configTree: ObjectNode, parentCourse: Course, sectionFolder: String): ObjectNode = runMigrationSteps(configTree) {
    migrateSection(mapper, it, parentCourse, sectionFolder)
  }

  fun migrateLesson(configTree: ObjectNode, parentItem: StudyItem, lessonFolder: String): ObjectNode = runMigrationSteps(configTree) {
    migrateLesson(mapper, it, parentItem, lessonFolder)
  }

  fun migrateTask(configTree: ObjectNode, parentLesson: Lesson, taskFolder: String): ObjectNode = runMigrationSteps(configTree) {
    migrateTask(mapper, it, parentLesson, taskFolder)
  }

  private fun runMigrationSteps(configTree: ObjectNode, migrateItem: YamlMigrationStep.(ObjectNode) -> ObjectNode): ObjectNode {
    val yamlVersion = getInitialYamlVersion()
    if (yamlVersion == null) {
      LOG.severe("Failed to get current yaml version to migrate an item")
      return configTree
    }

    var migratedConfig = configTree
    for (version in yamlVersion..CURRENT_YAML_VERSION) {
      val nextStep = migrationSteps[version] ?: continue
      migratedConfig = nextStep.migrateItem(migratedConfig)
    }

    return migratedConfig
  }

  companion object {
    private val LOG = logger<YamlMigrator>()
  }
}