package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.MAPPER
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.REMOTE_MAPPER
import com.jetbrains.edu.coursecreator.yaml.format.RemoteStudyItem
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [YamlLoader.loadItem].
 */
object YamlDeserializer {

  fun deserializeItem(configFile: VirtualFile): StudyItem {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return when (configName) {
      COURSE_CONFIG -> deserialize(configFileText, Course::class.java)
      SECTION_CONFIG -> deserialize(configFileText, Section::class.java)
      LESSON_CONFIG -> deserializeLesson(configFileText)
      TASK_CONFIG -> deserializeTask(configFileText)
      else -> unexpectedConfigFileError(configName)
    }
  }

  inline fun <reified T : StudyItem> StudyItem.deserializeContent(project: Project, contentList: MutableList<T>): List<T> {
    val parentDir = getDir(project) ?: noItemDirError(name)

    return contentList.map { titledItem ->
      val configFile = titledItem.findConfigFile(parentDir, *childrenConfigFileNames)
      val deserializeItem = deserializeItem(configFile) as T
      deserializeItem.name = titledItem.name
      deserializeItem
    }
  }

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [com.jetbrains.edu.coursecreator.yaml.format.CourseBuilder]
   */
  @VisibleForTesting
  fun <T : ItemContainer> deserialize(configFileText: String, clazz: Class<T>): T = MAPPER.readValue(configFileText, clazz)

  @VisibleForTesting
  fun deserializeLesson(configFileText: String): Lesson {
    val treeNode = MAPPER.readTree(configFileText)
    val type = asText(treeNode.get("type"))
    val clazz = when (type) {
      "framework" -> FrameworkLesson::class.java
      null -> Lesson::class.java
      else -> formatError("Unsupported lesson type '$type'")
    }
    return MAPPER.treeToValue(treeNode, clazz)
  }

  @VisibleForTesting
  fun deserializeTask(configFileText: String): Task {
    val treeNode = MAPPER.readTree(configFileText)
    val type = asText(treeNode.get("type")) ?: formatError("Task type not specified")

    val clazz = when (type) {
      "edu" -> EduTask::class.java
      "output" -> OutputTask::class.java
      "theory" -> TheoryTask::class.java
      "choice" -> ChoiceTask::class.java
      else -> formatError("Unsupported task type '$type'")
    }
    return MAPPER.treeToValue(treeNode, clazz)
  }

  fun deserializeRemoteItem(configFile: VirtualFile): StudyItem {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return when (configName) {
      REMOTE_COURSE_CONFIG -> REMOTE_MAPPER.readValue(configFileText, EduCourse::class.java)
      REMOTE_LESSON_CONFIG -> REMOTE_MAPPER.readValue(configFileText, Lesson::class.java)
      REMOTE_SECTION_CONFIG,
      REMOTE_TASK_CONFIG -> REMOTE_MAPPER.readValue(configFileText, RemoteStudyItem::class.java)
      else -> unexpectedConfigFileError(configName)
    }
  }

  private fun asText(node: JsonNode?): String? {
    return if (node == null || node.isNull) null else node.asText()
  }

  val StudyItem.childrenConfigFileNames: Array<String>
    get() = when (this) {
      is Course -> arrayOf(SECTION_CONFIG, LESSON_CONFIG)
      is Section -> arrayOf(LESSON_CONFIG)
      is Lesson -> arrayOf(TASK_CONFIG)
      else -> error("Unexpected StudyItem: ${javaClass.simpleName}")
    }

  fun StudyItem.findConfigFile(parentDir: VirtualFile, vararg configFileNames: String): VirtualFile {
    val itemDir = parentDir.findChild(name) ?: noItemDirError(name)
    return configFileNames.map { itemDir.findChild(it) }.firstOrNull { it != null } ?: noConfigFileError(itemDir.name)
  }

  private val VirtualFile.document
    get() = FileDocumentManager.getInstance().getDocument(this) ?: error("Cannot find document for a file: ${name}")

  fun noItemDirError(itemName: String): Nothing = error("Cannot find directory for item: '$itemName'")

  fun noConfigFileError(itemName: String, configFileName: String = ""): Nothing = error(
    "Cannot find config file $configFileName for item: '$itemName'")

  fun formatError(message: String): Nothing = throw InvalidYamlFormatException(message)

  private fun unexpectedConfigFileError(configName: String): Nothing = error("Unexpected config file: $configName")
}