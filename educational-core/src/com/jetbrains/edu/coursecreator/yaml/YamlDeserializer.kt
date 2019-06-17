package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
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
import com.jetbrains.edu.coursecreator.yaml.YamlLoader.getEditor
import com.jetbrains.edu.coursecreator.yaml.format.RemoteStudyItem
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import org.yaml.snakeyaml.composer.ComposerException
import org.yaml.snakeyaml.constructor.ConstructorException
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.parser.ParserException
import org.yaml.snakeyaml.scanner.ScannerException

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [YamlLoader.loadItem].
 */
object YamlDeserializer {

  fun deserializeItem(project: Project, configFile: VirtualFile): StudyItem? {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return try {
      when (configName) {
        COURSE_CONFIG -> deserialize(configFileText, Course::class.java)
        SECTION_CONFIG -> deserialize(configFileText, Section::class.java)
        LESSON_CONFIG -> deserializeLesson(configFileText)
        TASK_CONFIG -> deserializeTask(configFileText)
        else -> loadingError(unknownConfigMessage(configFile.name))
      }
    }
    catch (e: Exception) {
      processErrors(project, configFile, e)
      return null
    }
  }

  inline fun <reified T : StudyItem> StudyItem.deserializeContent(project: Project, contentList: MutableList<T>): List<T> {
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile: VirtualFile = getConfigFileForChild(project, titledItem.name) ?: continue
      val deserializeItem = deserializeItem(project, configFile) as? T ?: continue
      deserializeItem.name = titledItem.name
      content.add(deserializeItem)
    }

    return content
  }

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [com.jetbrains.edu.coursecreator.yaml.format.CourseBuilder]
   */
  @VisibleForTesting
  fun <T : ItemContainer> deserialize(configFileText: String, clazz: Class<T>): T? = MAPPER.readValue(configFileText, clazz)

  @VisibleForTesting
  fun deserializeLesson(configFileText: String): Lesson {
    val treeNode = MAPPER.readTree(configFileText)
    val type = asText(treeNode.get("type"))
    val clazz = when (type) {
      "framework" -> FrameworkLesson::class.java
      null -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, "lesson"))
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
      else -> formatError(unsupportedItemTypeMessage(type, "task"))
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
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  private fun asText(node: JsonNode?): String? {
    return if (node == null || node.isNull) null else node.asText()
  }

  private val StudyItem.childrenConfigFileNames: Array<String>
    get() = when (this) {
      is Course -> arrayOf(SECTION_CONFIG, LESSON_CONFIG)
      is Section -> arrayOf(LESSON_CONFIG)
      is Lesson -> arrayOf(TASK_CONFIG)
      else -> error("Unexpected StudyItem: ${javaClass.simpleName}")
    }

  fun StudyItem.getConfigFileForChild(project: Project, childName: String): VirtualFile? {
    val dir = getDir(project) ?: error(noDirForItemMessage(name))
    val itemDir = dir.findChild(childName)
    val configFile = childrenConfigFileNames.map { itemDir?.findChild(it) }.firstOrNull { it != null }
    if (configFile != null) {
      return configFile
    }

    val message = if (itemDir == null) "Directory for item: '$childName' not found" else "Config file for item: '${childName}' not found"
    val notification = Notification("Edu.InvalidConfig", "Config file not found", message, NotificationType.ERROR)
    notification.notify(project)
    return null
  }

  private val VirtualFile.document
    get() = FileDocumentManager.getInstance().getDocument(this) ?: error("Cannot find document for a file: ${name}")

  private fun processErrors(project: Project, configFile: VirtualFile, e: Exception) {
    @Suppress("DEPRECATION")
    // suppress deprecation for MarkedYAMLException as it is actually thrown from com.fasterxml.jackson.dataformat.yaml.YAMLParser.nextToken
    when (e) {
      is MissingKotlinParameterException -> {
        val parameterName = e.parameter.name
        if (parameterName == null) {
          showError(project, configFile)
        }
        else {
          showError(project, configFile,
                    "${NameUtil.nameToWordsLowerCase(parameterName).joinToString("_")} is empty")
        }
      }
      is InvalidYamlFormatException -> showError(project, configFile, e.message.capitalize())
      is IllegalStateException -> {
        showError(project, configFile)
      }
      is MismatchedInputException -> {
        showError(project, configFile)
      }
      is YAMLException -> {
        val cause = e.cause
        val message = when (cause) {
          is ScannerException -> {
            yamlParsingErrorNotificationMessage(cause.problem, cause.contextMark.line)
          }
          is ParserException -> {
            yamlParsingErrorNotificationMessage(cause.problem, cause.contextMark.line)
          }
          is ConstructorException -> {
            yamlParsingErrorNotificationMessage(cause.problem, cause.contextMark.line)
          }
          is ComposerException -> {
            yamlParsingErrorNotificationMessage(cause.problem, cause.contextMark.line)
          }
          else -> {
            cause?.message?.capitalize()
          }
        }
        if (message != null) {
          showError(project, configFile, message)
        }
        else {
          showError(project, configFile)
        }
      }
      is com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException -> {
        val message = yamlParsingErrorNotificationMessage(e.problem, e.contextMark?.line)
        if (message != null) {
          showError(project, configFile, message)
        }
        else {
          showError(project, configFile)
        }
      }
      is JsonMappingException -> {
        val causeException = e.cause
        if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
          showError(project, configFile)
        }
        else {
          showError(project, configFile, causeException.message)
        }
      }
      else -> throw e
    }
  }

  private fun yamlParsingErrorNotificationMessage(problem: String?, line: Int?) =
    if (problem != null && line != null) "$problem at line ${line + 1}" else null

  fun showError(project: Project, configFile: VirtualFile, cause: String = "invalid config") {
    val editor = configFile.getEditor(project)
    if (editor != null) {
      editor.headerComponent = InvalidFormatPanel(cause)
    }
    else {
      val notification = InvalidConfigNotification(project, configFile, cause)
      notification.notify(project)
    }
  }
}