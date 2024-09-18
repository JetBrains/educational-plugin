package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION
import org.jetbrains.annotations.VisibleForTesting

class YamlMigrator private constructor(private val loadedYamlVersion: Int, private val configTree: ObjectNode) {

  fun needToMigrate(): Boolean = loadedYamlVersion < CURRENT_YAML_VERSION

  /**
   * This method is supposed to fix the structure of YAML if it changed between versions.
   * It is called before the course if loaded, and should operate over configTree.
   */
  fun migrateStructure() {
    // TODO migrate configTree to a new tree and save it to course-info.yaml
  }

  fun migrateModel(course: Course) {
    if (!needToMigrate()) return

    for (version in loadedYamlVersion + 1..CURRENT_YAML_VERSION) {
      updateModelToVersion(version, course)
    }
  }

  @VisibleForTesting
  fun updateModelToVersion(version: Int, course: Course) {
    when (version) {
      1 -> {} // Do nothing. The 1st version only adds "yaml_version: 1" to the end of config.
    }
  }

  companion object {
    private val YAML_MAPPER = ObjectMapper(YAMLFactory())

    fun getInstance(project: Project): YamlMigrator? {
      val courseDir = project.courseDir
      val configFile = courseDir.findChild(YamlConfigSettings.COURSE_CONFIG)

      if (configFile == null) {
        logger<YamlMigrator>().warn("Failed to find course-info.yaml config file")
        return null
      }

      val configText = runReadAction {
        VfsUtil.loadText(configFile)
      }
      
      return getInstance(configText)
    }

    fun getInstance(configText: String): YamlMigrator? {
      val configTree = YAML_MAPPER.readTree(configText) as? ObjectNode

      if (configTree == null) {
        logger<YamlMigrator>().warn("Failed course-info.yaml to load config file")
        return null
      }

      val version = configTree.get(YAML_VERSION)?.asInt(0) ?: 0

      if (version > CURRENT_YAML_VERSION) {
        logger<YamlMigrator>().warn("YAML version of the project is $version which is greater than the latest supported version $CURRENT_YAML_VERSION")
      }

      return YamlMigrator(version, configTree)
    }
  }
}
