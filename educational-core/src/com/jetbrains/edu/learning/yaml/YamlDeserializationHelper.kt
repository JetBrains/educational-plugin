package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.CourseMode.Companion.toCourseMode
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.errorHandling.InvalidConfigNotification
import com.jetbrains.edu.learning.yaml.errorHandling.noDirForItemMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import org.jetbrains.annotations.NonNls

object YamlDeserializationHelper {
  private val StudyItem.childrenConfigFileNames: Array<String>
    get() = when (this) {
      is Course -> arrayOf(YamlFormatSettings.SECTION_CONFIG, YamlFormatSettings.LESSON_CONFIG)
      is Section -> arrayOf(YamlFormatSettings.LESSON_CONFIG)
      is Lesson -> arrayOf(YamlFormatSettings.TASK_CONFIG)
      else -> error("Unexpected StudyItem: ${javaClass.simpleName}")
    }


  fun StudyItem.getConfigFileForChild(project: Project, childName: String): VirtualFile? {
    val dir = getDir(project.courseDir) ?: error(noDirForItemMessage(name))
    val itemDir = dir.findChild(childName)
    val configFile = childrenConfigFileNames.map { itemDir?.findChild(it) }.firstOrNull { it != null }
    if (configFile != null) {
      return configFile
    }

    val message = if (itemDir == null) {
      EduCoreBundle.message("yaml.editor.notification.directory.not.found", childName)
    }
    else {
      EduCoreBundle.message("yaml.editor.notification.config.file.not.found", childName)
    }

    @NonNls
    val errorMessageToLog = "Config file for currently loading item ${name} not found"
    val parentConfig = dir.findChild(configFileName) ?: error(errorMessageToLog)
    showError(project, null, parentConfig, message)

    return null
  }

  fun showError(
    project: Project,
    originalException: Exception?,
    configFile: VirtualFile,
    cause: String = EduCoreBundle.message("yaml.editor.notification.invalid.config"),
  ) {
    // to make test failures more comprehensible
    if (isUnitTestMode && project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION) == true) {
      if (originalException != null) {
        throw YamlDeserializerBase.ProcessedException(cause, originalException)
      }
    }
    runInEdt {
      val editor = configFile.getEditor(project)
      ApplicationManager.getApplication().messageBus.syncPublisher(YamlDeserializerBase.YAML_LOAD_TOPIC).yamlFailedToLoad(configFile, cause)
      if (editor == null) {
        val notification = InvalidConfigNotification(project, configFile, cause)
        notification.notify(project)
      }
    }
  }

  fun getCourseMode(courseConfigText: String): CourseMode? {
    val treeNode = YamlFormatSynchronizer.MAPPER.readTree(courseConfigText)
    val courseModeText = asText(treeNode.get(YamlMixinNames.MODE))
    return courseModeText?.toCourseMode()
  }

  fun getCourseType(courseConfigText: String): String? {
    val treeNode = YamlFormatSynchronizer.MAPPER.readTree(courseConfigText)
    return asText(treeNode.get(YamlMixinNames.TYPE))
  }

  fun asText(node: JsonNode?): String? {
    return if (node == null || node.isNull) null else node.asText()
  }


}