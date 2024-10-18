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

const val YAML_VERSION_MAPPER_KEY = "yaml_version"
object YamlVersionMapperKey : InjectableValueKey<Int>(YAML_VERSION_MAPPER_KEY)

/**
 * Migrate methods [migrateSection], [migrateLesson], [migrateTask] receive an already deserialized parent item as an argument.
 * The parent item also has its parents, so during deserialization there is access to all the parents, including the course.
 */
class YamlMigrator(private val mapper: ObjectMapper) {

  val migrationSteps = mapOf(
    1 to ToVersion1Step,
  )

  private fun getInitialYamlVersion(): Int? = mapper.getEduValue(YamlVersionMapperKey)

  fun needMigration(): Boolean {
    val yamlVersion = getInitialYamlVersion()
    return yamlVersion != null && yamlVersion < CURRENT_YAML_VERSION
  }

  /**
   * Migrates tree of the course config, populates the mapper with the original YAML version
   */
  fun migrateCourse(configTree: ObjectNode): ObjectNode {
    val yamlVersion = configTree.get(YAML_VERSION)?.asInt(0) ?: 0
    mapper.setEduValue(YamlVersionMapperKey, yamlVersion)

    return runMigrationSteps(configTree) {
      this.migrateCourse(it)
    }

    configTree.put(YAML_VERSION, CURRENT_YAML_VERSION)
  }

  fun migrateSection(configTree: ObjectNode, parentCourse: Course): ObjectNode = runMigrationSteps(configTree) {
    this.migrateSection(it, parentCourse)
  }

  fun migrateLesson(configTree: ObjectNode, parentItem: StudyItem): ObjectNode = runMigrationSteps(configTree) {
    this.migrateLesson(it, parentItem)
  }

  fun migrateTask(configTree: ObjectNode, parentLesson: Lesson): ObjectNode = runMigrationSteps(configTree) {
    this.migrateTask(it, parentLesson)
  }

  private fun runMigrationSteps(configTree: ObjectNode, migrateItem: YamlMigrationStep.(ObjectNode) -> ObjectNode): ObjectNode {
    val yamlVersion = getInitialYamlVersion()
    if (yamlVersion == null) {
      LOG.severe("Failed to get current yaml version to migrate an item")
      return configTree
    }

    var migratedConfig = configTree
    for (version in yamlVersion..CURRENT_YAML_VERSION) {
      val nextStep = migrationSteps[version]
      migratedConfig = nextStep?.migrateItem(migratedConfig) ?: migratedConfig
    }

    return migratedConfig
  }

  companion object {
    private val LOG = logger<YamlMigrator>()
  }
}