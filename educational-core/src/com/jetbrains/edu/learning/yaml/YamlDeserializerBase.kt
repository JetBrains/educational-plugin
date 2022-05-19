package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission.CHECK_IO_MISSION_TASK_TYPE
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
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.yaml.YamlDeserializationHelper.asText
import com.jetbrains.edu.learning.yaml.YamlDeserializationHelper.showError
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
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
open class YamlDeserializerBase : YamlDeserializer() {

  override fun deserializeItem(configFile: VirtualFile, project: Project?, mapper: ObjectMapper): StudyItem? {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return try {
      when (configName) {
        COURSE_CONFIG -> deserializeCourse(mapper, configFileText)
        SECTION_CONFIG -> deserializeSection(mapper, configFileText)
        LESSON_CONFIG -> deserializeLesson(mapper, configFileText)
        TASK_CONFIG -> deserializeTask(mapper, configFileText)
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

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [com.jetbrains.edu.learning.yaml.format.CourseBuilder]
   */
  override fun deserializeCourse(mapper: ObjectMapper, configFileText: String): Course {
    val treeNode = mapper.readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val courseMode = asText(treeNode.get("mode"))
    val course = mapper.treeToValue(treeNode, Course::class.java)
    course.courseMode = if (courseMode != null) CourseMode.STUDENT else CourseMode.EDUCATOR
    return course
  }

  override fun deserializeSection(mapper: ObjectMapper, configFileText: String): Section {
    val treeNode = mapper.readNode(configFileText)
    return mapper.treeToValue(treeNode, Section::class.java)
  }

  override fun deserializeLesson(mapper: ObjectMapper, configFileText: String): Lesson {
    val treeNode = mapper.readNode(configFileText)

    val type = asText(treeNode.get(YamlMixinNames.TYPE))
    return mapper.treeToValue(treeNode, when (type) {
      FrameworkLesson().itemType -> FrameworkLesson::class.java
      null, Lesson().itemType -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.LESSON))
    })
  }

  private fun ObjectMapper.readNode(configFileText: String): JsonNode =
    when (val tree = readTree(configFileText)) {
      null -> JsonNodeFactory.instance.objectNode()
      is MissingNode -> JsonNodeFactory.instance.objectNode()
      else -> tree
    }

  override fun deserializeTask(mapper: ObjectMapper, configFileText: String): Task {
    val treeNode = mapper.readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = asText(treeNode.get(YamlMixinNames.TYPE)) ?: formatError(
      EduCoreBundle.message("yaml.editor.invalid.task.type.not.specified"))

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
      STRING_TASK_TYPE -> StringTask::class.java
      NUMBER_TASK_TYPE -> NumberTask::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.TASK))
    }
    return mapper.treeToValue(treeNode, clazz)
  }

  override fun deserializeRemoteItem(configFile: VirtualFile): StudyItem {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return when (configName) {
      REMOTE_COURSE_CONFIG -> deserializeCourseRemoteInfo(configFileText)
      REMOTE_LESSON_CONFIG -> deserializeLessonRemoteInfo(configFileText)
      REMOTE_SECTION_CONFIG -> deserializeRemoteStudyItem(configFileText)
      REMOTE_TASK_CONFIG -> deserializeTaskRemoteInfo(configFileText)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  private fun deserializeRemoteStudyItem(configFileText: String) = REMOTE_MAPPER.readValue(configFileText, RemoteStudyItem::class.java)

  protected open fun deserializeCourseRemoteInfo(configFileText: String): Course {
    return REMOTE_MAPPER.readValue(configFileText, EduCourse::class.java)
  }

  protected open fun deserializeLessonRemoteInfo(configFileText: String): StudyItem = deserializeRemoteStudyItem(configFileText)

  protected open fun deserializeTaskRemoteInfo(configFileText: String): StudyItem = deserializeRemoteStudyItem(configFileText)

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


  @VisibleForTesting
  class ProcessedException(message: String, originalException: Exception?) : Exception(message, originalException)


  companion object {
    @NonNls
    private const val TOPIC = "Loaded YAML"

    val YAML_LOAD_TOPIC: Topic<YamlListener> = Topic.create(TOPIC, YamlListener::class.java)
  }
}
