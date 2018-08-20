package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.*
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.*


@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(*arrayOf(
  JsonSubTypes.Type(value = EduCourse::class, name = "course"),
  JsonSubTypes.Type(value = Section::class, name = "section"),
  JsonSubTypes.Type(value = Lesson::class, name = "lesson")
))
abstract class StudyItemMixin


@JsonIgnoreProperties(value = *arrayOf(
  "change_notes", "course", "tags"
))
@JsonTypeName("course")
abstract class CourseMixin {

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("summary")
  lateinit var description: String

  @JsonProperty("language")
  lateinit var myLanguageCode: String

  @JsonProperty("programming_language")
  lateinit var myProgrammingLanguage: String

  @JsonProperty("items")
  lateinit var items: List<StudyItem>

  @JsonIgnore
  abstract fun getLanguage(): String

  @JsonProperty("course_files")
  lateinit var courseFiles: HashMap<String, String>

}


@JsonIgnoreProperties(value = *arrayOf(
  "format", "description", "description_format"
))
@JsonTypeName("section")
abstract class SectionMixin {

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var id: Int = 0

  @JsonProperty("last_modified")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  lateinit var myUpdateDate: Date

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("items")
  lateinit var items: List<StudyItem>

}


@JsonIgnoreProperties(value = *arrayOf(
  "format", "description", "description_format"
))
@JsonTypeName("lesson")
abstract class LessonMixin {

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var myId: Int = 0

  @JsonProperty("last_modified")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  lateinit var myUpdateDate: Date

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("items")
  lateinit var taskList: List<Task>

}
