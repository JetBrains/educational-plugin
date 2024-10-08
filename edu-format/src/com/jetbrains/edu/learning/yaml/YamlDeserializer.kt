package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.CourseMode.Companion.toCourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask.Companion.TABLE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask.Companion.UNSUPPORTED_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask.Companion.CHOICE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask.Companion.MATCHING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask.Companion.SORTING_TASK_TYPE
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlMapper.basicMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.remoteMapper
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.loadingError
import com.jetbrains.edu.learning.yaml.errorHandling.unknownConfigMessage
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItem
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TASK
import org.jetbrains.annotations.VisibleForTesting

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [com.jetbrains.edu.learning.yaml.YamlLoader.loadItem].
 */
object YamlDeserializer {
  fun deserializeItem(configName: String, mapper: ObjectMapper, configFileText: String): StudyItem {
    return when (configName) {
      COURSE_CONFIG -> mapper.deserializeCourse(configFileText)
      SECTION_CONFIG -> mapper.deserializeSection(configFileText)
      LESSON_CONFIG -> mapper.deserializeLesson(configFileText)
      TASK_CONFIG -> mapper.deserializeTask(configFileText)
      else -> loadingError(unknownConfigMessage(configName))
    }
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
    course.courseMode = if (courseMode != null) CourseMode.STUDENT else CourseMode.EDUCATOR
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

    val type = asText(treeNode.get(YamlMixinNames.TYPE))
    val clazz = when (type) {
      FrameworkLesson().itemType -> FrameworkLesson::class.java
      StepikLesson().itemType -> StepikLesson::class.java
      null, Lesson().itemType -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, LESSON))
    }
    return treeToValue(treeNode, clazz)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeTask(configFileText: String): Task {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = asText(treeNode.get(YamlMixinNames.TYPE))
               ?: formatError(message("yaml.editor.invalid.task.type.not.specified"))

    val clazz = when (type) {
      EDU_TASK_TYPE -> EduTask::class.java
      REMOTE_EDU_TASK_TYPE -> RemoteEduTask::class.java
      OUTPUT_TASK_TYPE -> OutputTask::class.java
      THEORY_TASK_TYPE -> TheoryTask::class.java
      DATA_TASK_TYPE -> DataTask::class.java
      CHOICE_TASK_TYPE -> ChoiceTask::class.java
      IDE_TASK_TYPE -> IdeTask::class.java
      // for student mode
      CODE_TASK_TYPE -> CodeTask::class.java
      STRING_TASK_TYPE -> StringTask::class.java
      NUMBER_TASK_TYPE -> NumberTask::class.java
      UNSUPPORTED_TASK_TYPE -> UnsupportedTask::class.java
      MATCHING_TASK_TYPE -> MatchingTask::class.java
      SORTING_TASK_TYPE -> SortingTask::class.java
      TABLE_TASK_TYPE -> TableTask::class.java
      else -> formatError(unsupportedItemTypeMessage(type, TASK))
    }
    return treeToValue(treeNode, clazz)
  }

  fun deserializeRemoteItem(configName: String, configFileText: String): StudyItem {
    return when (configName) {
      REMOTE_COURSE_CONFIG -> deserializeCourseRemoteInfo(configFileText)
      REMOTE_LESSON_CONFIG -> deserializeLessonRemoteInfo(configFileText)
      REMOTE_SECTION_CONFIG -> remoteMapper().readValue(configFileText, RemoteStudyItem::class.java)
      REMOTE_TASK_CONFIG -> deserializeTaskRemoteInfo(configFileText)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  private fun deserializeCourseRemoteInfo(configFileText: String): Course {
    val remoteMapper = remoteMapper()
    val treeNode = remoteMapper.readTree(configFileText)

    val clazz = if (treeNode.get(YamlMixinNames.HYPERSKILL_PROJECT) != null) {
      HyperskillCourse::class.java
    }
    else {
      EduCourse::class.java
    }

    return remoteMapper.treeToValue(treeNode, clazz)
  }

  private fun deserializeLessonRemoteInfo(configFileText: String): StudyItem {
    val treeNode = remoteMapper().readTree(configFileText)
    return remoteMapper().treeToValue(treeNode, RemoteStudyItem::class.java)
  }

  private fun deserializeTaskRemoteInfo(configFileText: String): StudyItem {
    val treeNode = remoteMapper().readTree(configFileText)

    val clazz = when (asText(treeNode.get(YamlMixinNames.TYPE))) {
      DATA_TASK_TYPE -> DataTask::class.java
      else -> RemoteStudyItem::class.java
    }

    return remoteMapper().treeToValue(treeNode, clazz)
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

  fun getCourseMode(courseConfigText: String): CourseMode? {
    val treeNode = basicMapper().readTree(courseConfigText)
    val courseModeText = asText(treeNode.get(YamlMixinNames.MODE))
    return courseModeText?.toCourseMode()
  }

}
