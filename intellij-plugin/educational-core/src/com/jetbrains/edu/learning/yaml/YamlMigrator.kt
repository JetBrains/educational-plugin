package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intellij.lang.Language
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYCHARM
import com.jetbrains.edu.learning.courseFormat.Language.findLanguageByName
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.stepik.api.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.reformatYaml
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION

class YamlMigrator(val project: Project) {

  fun migrate() {
    val configFile = getConfigFile() ?: return
    var configTree = readConfigTree(configFile) ?: return
    var version = configTree.get(YAML_VERSION)?.asInt(0) ?: 0

    if (version >= CURRENT_YAML_VERSION) return

    val configurator = getConfigurator(configTree) ?: return

    while (version < CURRENT_YAML_VERSION) {
      version++
      // we currently update only the course-info.yaml
      // this should be rewritten, when we want to update also other yamls
      configTree = updateYamlToVersion(version, configurator, configTree)
    }

    writeConfigTree(configTree, configFile)
  }

  private fun getConfigFile(): VirtualFile? {
    val courseDir = project.courseDir
    return courseDir.findChild(YamlConfigSettings.COURSE_CONFIG)
  }

  private fun readConfigTree(configFile: VirtualFile): ObjectNode? {
    val configText = VfsUtil.loadText(configFile)
    val generalYamlMapper = YAML_MAPPER
    val configTree = generalYamlMapper.readTree(configText) ?: return null

    return configTree as? ObjectNode
  }

  // This is the same logic as in com.jetbrains.edu.learning.courseFormat.ext.CourseExt.getConfigurator
  private fun getConfigurator(configTree: ObjectNode): EduConfigurator<*>? {
    val displayProgrammingLanguageName = configTree.get(PROGRAMMING_LANGUAGE)?.asText() ?: return null
    val languageId = findLanguageByName(displayProgrammingLanguageName) ?: return null
    val languageById = Language.findLanguageByID(languageId) ?: return null

    val environment = configTree.get(ENVIRONMENT)?.asText() ?: DEFAULT_ENVIRONMENT

    return EduConfiguratorManager.findExtension(PYCHARM, environment, languageById)?.instance
  }

  private fun updateYamlToVersion(version: Int, configurator: EduConfigurator<*>, configTree: ObjectNode): ObjectNode {
    val updatedConfigTree = when (version) {
      1 -> updateToVersion1(configTree, configurator)
      else -> configTree
    }

    return updatedConfigTree.set(YAML_VERSION, IntNode(version))
  }

  /**
   * This update adds additional files to the course
   */
  private fun updateToVersion1(configTree: ObjectNode, configurator: EduConfigurator<*>): ObjectNode {
    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(configurator, project, saveDocuments = false)

    val additionalFilesNode = YAML_MAPPER.createArrayNode()

    for (additionalFile in additionalFiles) {
      val additionalFileNode = YAML_MAPPER.createObjectNode()
      additionalFileNode.put(NAME, additionalFile.name)

      additionalFilesNode.add(additionalFileNode)
    }

    return configTree.set(ADDITIONAL_FILES, additionalFilesNode)
  }

  private fun writeConfigTree(configTree: ObjectNode, configFile: VirtualFile) {
    val textConfig = YamlMapper.MAPPER.writeValueAsString(configTree)
    project.invokeLater {
      runWriteAction {
        configFile.writeText(reformatYaml(project, textConfig))
        FileDocumentManager.getInstance().reloadFiles(configFile)
      }
    }
  }

  companion object {
    val YAML_MAPPER = ObjectMapper(YAMLFactory())
  }
}
