package com.jetbrains.edu.learning.yaml.migration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.reformatYaml
import com.jetbrains.edu.learning.yaml.YamlMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.Files

class YamlMigrator private constructor(
  private val project: Project,
  private val loadedYamlVersion: Int,
  private val courseDir: VirtualFile,
  private val courseTree: ObjectNode
) {

  fun needToMigrate(): Boolean = loadedYamlVersion < CURRENT_YAML_VERSION

  private val steps: List<Pair<Int, YamlMigrationStep>> = listOf(
    // 1 to ToVersion1Updater
    // 2 to ToVersion2Updater
  )

  /**
   * Stores updated versions of YAMLs before writing them to disk
   */
  private val migratedConfigs = mutableMapOf<VirtualFile, String>()

  /**
   * This method is supposed to fix the structure of YAML if it changed between versions.
   * It is called before the course loaded and operates over the YAML tree.
   */
  fun migrateStructure() {
    if (!needToMigrate()) return

    logger<YamlMigrator>().info("Migrating YAML configs from version $loadedYamlVersion to $CURRENT_YAML_VERSION")

    try {
      for (version in loadedYamlVersion + 1..CURRENT_YAML_VERSION) {
        updateStructureToVersion(version)
      }

      for ((file, fileContents) in migratedConfigs) {
        val formattedContents = reformatYaml(project, file.name, fileContents)
        Files.writeString(file.toNioPath(), formattedContents)
      }
    }
    catch (th: Throwable) {
      logger<YamlMigrator>().error("Failed to migrate YAML", th)
    }
  }

  @VisibleForTesting
  fun updateStructureToVersion(version: Int) {
    for ((stepVersion, migrationStep) in steps) {
      if (stepVersion == version && migrationStep.migrationNeeded(project, courseTree)) {
        performStep(migrationStep)
      }
    }

    performStep(NewVersionWriter(version))
  }

  private fun performStep(migrationStep: YamlMigrationStep) {
    val configFileName = migrationStep.getConfigName()
    VfsUtil.iterateChildrenRecursively(
      courseDir,
      object : VirtualFileFilter {
        override fun accept(file: VirtualFile) = file.isDirectory || file.isFile && file.name == configFileName
      },
      object : ContentIterator {
        override fun processFile(configFile: VirtualFile): Boolean {
          if (configFile.isDirectory) return true

          val configYaml = loadOrGetAlreadyMigrated(configFile)
          val configTree = YAML_MAPPER.readTree(configYaml) as? ObjectNode ?: return true
          val transformedTree = migrationStep.transform(configTree)
          val transformedConfig = YamlMapper.MAPPER.writeValueAsString(transformedTree)
          migratedConfigs[configFile] = transformedConfig

          return true
        }
      })
  }

  private fun loadOrGetAlreadyMigrated(file: VirtualFile): String = migratedConfigs[file] ?: runReadAction {
    file.readText()
  }

  @VisibleForTesting
  fun migratedConfig(configPath: String): String? = migratedConfigs[courseDir.findFileByRelativePath(configPath)]

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

      val configTree = YAML_MAPPER.readTree(configText) as? ObjectNode

      if (configTree == null) {
        logger<YamlMigrator>().warn("Failed to load course-info.yaml config file")
        return null
      }

      val version = configTree.get(YAML_VERSION)?.asInt(0) ?: 0

      if (version > CURRENT_YAML_VERSION) {
        logger<YamlMigrator>().warn("YAML version of the project is $version which is greater than the latest supported version $CURRENT_YAML_VERSION")
      }

      return YamlMigrator(project, version, courseDir, configTree)
    }
  }
}
