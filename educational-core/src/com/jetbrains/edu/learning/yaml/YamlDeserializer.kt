package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission.CHECK_IO_MISSION_TASK_TYPE
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask.Companion.VIDEO_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask.Companion.CHOICE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.MAPPER
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.REMOTE_MAPPER
import com.jetbrains.edu.learning.yaml.errorHandling.*
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItem
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import org.jetbrains.annotations.NonNls

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [YamlLoader.loadItem].
 */
object YamlDeserializer {
  @NonNls
  private const val TOPIC = "Loaded YAML"
  val YAML_LOAD_TOPIC: Topic<YamlListener> = Topic.create(TOPIC, YamlListener::class.java)

  fun deserializeItem(configFile: VirtualFile, project: Project?, mapper: ObjectMapper = MAPPER): StudyItem? {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return try {
      when (configName) {
        COURSE_CONFIG -> mapper.deserializeCourse(configFileText)
        SECTION_CONFIG -> mapper.deserializeSection(configFileText)
        LESSON_CONFIG -> mapper.deserializeLesson(configFileText)
        TASK_CONFIG -> mapper.deserializeTask(configFileText)
        else -> loadingError(unknownConfigMessage(configFile.name))
      }
    }
    catch (e: Exception) {
      if (project != null) {
        processErrors(project, configFile, e)
      }
      return null
    }
  }

  inline fun <reified T : StudyItem> StudyItem.deserializeContent(project: Project,
                                                                  contentList: MutableList<T>,
                                                                  mapper: ObjectMapper = MAPPER): List<T> {
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile: VirtualFile = getConfigFileForChild(project, titledItem.name) ?: continue
      val deserializeItem = deserializeItem(configFile, project, mapper) as? T ?: continue
      deserializeItem.name = titledItem.name
      deserializeItem.index = titledItem.index
      content.add(deserializeItem)
    }

    return content
  }

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [com.jetbrains.edu.learning.yaml.format.CourseBuilder]
   */
  @VisibleForTesting
  fun ObjectMapper.deserializeCourse(configFileText: String): Course {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val courseMode = asText(treeNode.get("mode"))
    val course = treeToValue(treeNode, Course::class.java)
    course.courseMode = if (courseMode != null) CourseMode.STUDY else CourseMode.COURSE_MODE
    return course
  }

  private fun ObjectMapper.readNode(configFileText: String): JsonNode =
    when (val tree = readTree(configFileText)) {
      null -> JsonNodeFactory.instance.objectNode()
      is MissingNode -> JsonNodeFactory.instance.objectNode()
      else -> tree
    }

  @VisibleForTesting
  fun ObjectMapper.deserializeSection(configFileText: String): Section {
    val jsonNode = readNode(configFileText)
    return treeToValue(jsonNode, Section::class.java)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeLesson(configFileText: String): Lesson {
    val treeNode = readNode(configFileText)
    val type = asText(treeNode.get("type"))
    val clazz = when (type) {
      FrameworkLesson().itemType -> FrameworkLesson::class.java
      CheckiOStation().itemType -> CheckiOStation::class.java // for student projects only
      Lesson().itemType, null -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.LESSON))
    }
    return treeToValue(treeNode, clazz)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeTask(configFileText: String): Task {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = asText(treeNode.get("type")) ?: formatError(EduCoreBundle.message("yaml.editor.invalid.task.type.not.specified"))

    val clazz = when (type) {
      EDU_TASK_TYPE -> EduTask::class.java
      REMOTE_EDU_TASK_TYPE -> RemoteEduTask::class.java
      OUTPUT_TASK_TYPE -> OutputTask::class.java
      THEORY_TASK_TYPE -> TheoryTask::class.java
      DATA_TASK_TYPE -> DataTask::class.java
      VIDEO_TASK_TYPE -> VideoTask::class.java
      CHOICE_TASK_TYPE -> ChoiceTask::class.java
      IDE_TASK_TYPE -> IdeTask::class.java
      // for student mode
      CODE_TASK_TYPE -> CodeTask::class.java
      CHECK_IO_MISSION_TASK_TYPE -> CheckiOMission::class.java
      CODEFORCES_TASK_TYPE -> CodeforcesTask::class.java
      CODEFORCES_TASK_TYPE_WITH_FILE_IO -> CodeforcesTaskWithFileIO::class.java
      STRING_TASK_TYPE -> StringTask::class.java
      NUMBER_TASK_TYPE -> NumberTask::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.TASK))
    }
    return treeToValue(treeNode, clazz)
  }

  fun deserializeRemoteItem(configFile: VirtualFile): StudyItem {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return when (configName) {
      REMOTE_COURSE_CONFIG -> deserializeCourseRemoteInfo(configFileText)
      REMOTE_LESSON_CONFIG -> REMOTE_MAPPER.readValue(configFileText, Lesson::class.java)
      REMOTE_SECTION_CONFIG -> REMOTE_MAPPER.readValue(configFileText, RemoteStudyItem::class.java)
      REMOTE_TASK_CONFIG -> deserializeTaskRemoteInfo(configFileText)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  private fun deserializeCourseRemoteInfo(configFileText: String): Course {
    val treeNode = REMOTE_MAPPER.readTree(configFileText)
    val type = asText(treeNode.get("type"))

    val clazz = when {
      type == CodeforcesNames.CODEFORCES_COURSE_TYPE -> CodeforcesCourse::class.java
      treeNode.get(YamlMixinNames.HYPERSKILL_PROJECT) != null -> HyperskillCourse::class.java
      else -> EduCourse::class.java
    }

    return REMOTE_MAPPER.treeToValue(treeNode, clazz)
  }

  private fun deserializeTaskRemoteInfo(configFileText: String): StudyItem {
    val treeNode = REMOTE_MAPPER.readTree(configFileText)

    val clazz = when (asText(treeNode.get(YamlMixinNames.TYPE))) {
      DATA_TASK_TYPE -> DataTask::class.java
      else -> RemoteStudyItem::class.java
    }

    return REMOTE_MAPPER.treeToValue(treeNode, clazz)
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
          val cause = EduCoreBundle.message("yaml.editor.notification.parameter.is.empty",
                                            NameUtil.nameToWordsLowerCase(parameterName).joinToString("_"))
          showError(project, e, configFile, cause)
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

  // it doesn't require localization as `problems` is snakeyaml error message on which we have no influence
  @Suppress("UnstableApiUsage")
  @NlsSafe
  private fun yamlParsingErrorNotificationMessage(problem: String?, line: Int?) =
    if (problem != null && line != null) "$problem at line ${line + 1}" else null

  fun showError(project: Project,
                originalException: Exception?,
                configFile: VirtualFile,
                cause: String = EduCoreBundle.message("yaml.editor.notification.invalid.config")) {
    // to make test failures more comprehensible
    if (isUnitTestMode && project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION) == true) {
      if (originalException != null) {
        throw ProcessedException(cause, originalException)
      }
    }
    runInEdt {
      val editor = configFile.getEditor(project)
      ApplicationManager.getApplication().messageBus.syncPublisher(YAML_LOAD_TOPIC).yamlFailedToLoad(configFile, cause)
      if (editor == null) {
        val notification = InvalidConfigNotification(project, configFile, cause)
        notification.notify(project)
      }
    }
  }

  fun getCourseMode(courseConfigText: String): String? {
    val treeNode = MAPPER.readTree(courseConfigText)
    return asText(treeNode.get(YamlMixinNames.MODE))
  }

  @VisibleForTesting
  class ProcessedException(message: String, originalException: Exception?) : Exception(message, originalException)

}
