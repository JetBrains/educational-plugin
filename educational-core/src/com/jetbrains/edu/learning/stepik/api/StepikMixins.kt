package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.util.*

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
abstract class StepikEduCourseMixin {
  @JsonProperty("is_idea_compatible")
  var isCompatible = true

  @JsonProperty("course_format")
  lateinit var myType: String

  @JsonProperty("sections")
  lateinit var sectionIds: List<Int>

  @JsonProperty("instructors")
  lateinit var instructors: List<Int>

  @JsonProperty("id")
  private var id: Int = 0

  @JsonProperty("update_date")
  lateinit var myUpdateDate: Date

  @JsonProperty("is_public")
  var isPublic: Boolean = false

  @JsonProperty("summary")
  lateinit var description: String

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("programming_language")
  lateinit var myProgrammingLanguage: String

  @JsonProperty("language")
  lateinit var myLanguageCode: String
}

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikSectionMixin {
  @JsonProperty("units")
  lateinit var units: List<Int>

  @JsonProperty("course")
  var courseId: Int = 0

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("position")
  var position: Int = 0

  @JsonProperty("id")
  private var id: Int = 0

  @JsonProperty("update_date")
  lateinit var updateDate: Date
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikLessonMixin {
  @JsonProperty("id")
  var myId: Int = 0

  @JsonProperty("steps")
  lateinit var steps: MutableList<Int>

  @JsonProperty("is_public")
  var is_public: Boolean = false

  lateinit var myUpdateDate: Date

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("unit_id")
  var unitId: Int = 0
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepOptionsMixin {
  @JsonProperty("task_type")
  lateinit var taskType: String

  @JsonProperty("lesson_type")
  lateinit var lessonType: String

  @JsonProperty("title")
  lateinit var title: String

  @JsonProperty(SerializationUtils.Json.DESCRIPTION_TEXT)
  lateinit var descriptionText: String

  @JsonProperty(SerializationUtils.Json.DESCRIPTION_FORMAT)
  lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty("feedback_link")
  lateinit var myFeedbackLink: FeedbackLink

  @JsonProperty("files")
  lateinit var files: MutableList<TaskFile>

  @JsonProperty("samples")
  var samples: List<List<String>>? = null

  @JsonProperty("execution_memory_limit")
  var executionMemoryLimit: Int? = null

  @JsonProperty("execution_time_limit")
  var executionTimeLimit: Int? = null

  @JsonProperty("code_templates")
  var codeTemplates: Map<String, String>? = null

  @JsonProperty("format_version")
  var formatVersion: Int = JSON_FORMAT_VERSION
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikTaskFileMixin {
  @JsonProperty("name")
  lateinit var myName: String

  @JsonProperty("placeholders")
  lateinit var myAnswerPlaceholders: MutableList<AnswerPlaceholder>

  @JsonProperty("is_visible")
  var myVisible = true

  @JsonProperty("text")
  lateinit var myText : String
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikAnswerPlaceholderMixin {
  @JsonProperty("offset")
  var myOffset = -1

  @JsonProperty("length")
  private var myLength = -1

  @JsonProperty("dependency")
  lateinit var myPlaceholderDependency: AnswerPlaceholderDependency

  @JsonProperty("hints")
  lateinit var myHints: List<String>

  @JsonProperty("possible_answer")
  lateinit var myPossibleAnswer : String

  @JsonProperty("placeholder_text")
  lateinit var myPlaceholderText: String

  @JsonProperty("selected")
  private var mySelected = false
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikAnswerPlaceholderDependencyMixin {

  @JsonProperty("section")
  lateinit var sectionName: String

  @JsonProperty("lesson")
  lateinit var lessonName: String

  @JsonProperty("task")
  lateinit var taskName: String

  @JsonProperty("file")
  lateinit var fileName: String

  @JsonProperty("placeholder")
  var placeholderIndex: Int = 0

  @JsonProperty("is_visible")
  var isVisible = true
}

@Suppress("unused", "UNUSED_PARAMETER", "PropertyName") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE,
                creatorVisibility = JsonAutoDetect.Visibility.NONE)
class StepikFeedbackLinkMixin {
  @JsonProperty("link_type")
  lateinit var myType: FeedbackLink.LinkType

  @JsonProperty("link")
  lateinit var myLink: String
}