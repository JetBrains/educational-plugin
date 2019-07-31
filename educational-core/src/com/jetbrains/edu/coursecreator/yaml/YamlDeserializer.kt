package com.jetbrains.edu.coursecreator.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runInEdt
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
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.isUnitTestMode

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [YamlLoader.loadItem].
 */
object YamlDeserializer {

  fun deserializeItem(project: Project, configFile: VirtualFile, mapper: ObjectMapper = MAPPER): StudyItem? {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return try {
      when (configName) {
        COURSE_CONFIG -> mapper.deserialize(configFileText, Course::class.java)
        SECTION_CONFIG -> mapper.deserializeSection(configFileText)
        LESSON_CONFIG -> mapper.deserializeLesson(configFileText)
        TASK_CONFIG -> mapper.deserializeTask(configFileText)
        else -> loadingError(unknownConfigMessage(configFile.name))
      }
    }
    catch (e: Exception) {
      processErrors(project, configFile, e)
      return null
    }
  }

  inline fun <reified T : StudyItem> StudyItem.deserializeContent(project: Project,
                                                                  contentList: MutableList<T>,
                                                                  mapper: ObjectMapper = MAPPER): List<T> {
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile: VirtualFile = getConfigFileForChild(project, titledItem.name) ?: continue
      val deserializeItem = deserializeItem(project, configFile, mapper) as? T ?: continue
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
  fun <T : ItemContainer> ObjectMapper.deserialize(configFileText: String, clazz: Class<T>): T? = readValue(configFileText, clazz)

  @VisibleForTesting
  fun ObjectMapper.deserializeSection(configFileText: String): Section {
    val jsonNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    return treeToValue(jsonNode, Section::class.java)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeLesson(configFileText: String): Lesson {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = asText(treeNode.get("type"))
    val clazz = when (type) {
      FrameworkLesson().itemType -> FrameworkLesson::class.java
      Lesson().itemType, null -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.LESSON))
    }
    return treeToValue(treeNode, clazz)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeTask(configFileText: String): Task {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = asText(treeNode.get("type")) ?: formatError("Task type not specified")

    val clazz = when (type) {
      "edu" -> EduTask::class.java
      "output" -> OutputTask::class.java
      "theory" -> TheoryTask::class.java
      "choice" -> ChoiceTask::class.java
      "ide" -> IdeTask::class.java
      // for student mode
      "code" -> CodeTask::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.TASK))
    }
    return treeToValue(treeNode, clazz)
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

    val message = if (itemDir == null) "Directory for item '$childName' not found" else "Config file for item '${childName}' not found"
    val parentConfig = dir.findChild(configFileName) ?: error("Config file for currently loading item ${name} not found")
    showError(project, null, parentConfig, message)

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
          showError(project, e, configFile)
        }
        else {
          showError(project, e, configFile, "${NameUtil.nameToWordsLowerCase(parameterName).joinToString("_")} is empty")
        }
      }
      is InvalidYamlFormatException -> showError(project, e, configFile, e.message)
      is MismatchedInputException -> {
        showError(project, e, configFile)
      }
      is com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException -> {
        val message = yamlParsingErrorNotificationMessage(e.problem, e.contextMark?.line)
        if (message != null) {
          showError(project, e, configFile, message)
        }
        else {
          showError(project, e, configFile)
        }
      }
      is JsonMappingException -> {
        val causeException = e.cause
        if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
          showError(project, e, configFile)
        }
        else {
          showError(project, causeException, configFile, causeException.message)
        }
      }
      else -> throw e
    }
  }

  private fun yamlParsingErrorNotificationMessage(problem: String?, line: Int?) =
    if (problem != null && line != null) "$problem at line ${line + 1}" else null

  fun showError(project: Project,
                originalException: Exception?,
                configFile: VirtualFile,
                cause: String = "invalid config") {
    // to make test failures more comprehensible
    if (isUnitTestMode && project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION) == true) {
      if (originalException != null) {
        throw ProcessedException(cause, originalException)
      }
    }
    runInEdt {
      val editor = configFile.getEditor(project)
      if (editor != null) {
        editor.headerComponent = InvalidFormatPanel(project, cause)
      }
      else {
        val notification = InvalidConfigNotification(project, configFile, cause)
        notification.notify(project)
      }
    }
  }

  @VisibleForTesting
  class ProcessedException(message: String, originalException: Exception?) : Exception(message, originalException)
}