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


@JsonTypeName("course")
@JsonIgnoreProperties(value = *arrayOf( "change_notes", "course", "tags"))
abstract class CourseMixin {

  @JsonProperty(TITLE_FIELD)
  lateinit var name: String

  @JsonProperty(SUMMARY_FIELD)
  lateinit var description: String

  @JsonProperty(HUMAN_LANGUAGE_FIELD)
  lateinit var myLanguageCode: String

  @JsonProperty(PROGRAMMING_LANGUAGE_FIELD)
  lateinit var myProgrammingLanguage: String

  @JsonProperty(ITEMS_FILED)
  lateinit var items: List<StudyItem>

  @JsonProperty(COURSE_FILES_FIELD)
  lateinit var courseFiles: HashMap<String, String>

}


@JsonTypeName("section")
@JsonIgnoreProperties(value = *arrayOf("format", "description", "description_format"))
abstract class SectionMixin {

  @JsonProperty(ID_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var id: Int = 0

  @JsonProperty(LAST_MODIFIED_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  lateinit var myUpdateDate: Date

  @JsonProperty(TITLE_FIELD)
  lateinit var name: String

  @JsonProperty(ITEMS_FILED)
  lateinit var items: List<StudyItem>

}


@JsonTypeName("lesson")
@JsonIgnoreProperties(value = *arrayOf("format", "description", "description_format"))
abstract class LessonMixin {

  @JsonProperty(ID_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var myId: Int = 0

  @JsonProperty(LAST_MODIFIED_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  lateinit var myUpdateDate: Date

  @JsonProperty(TITLE_FIELD)
  lateinit var name: String

  @JsonProperty(ITEMS_FILED)
  lateinit var taskList: List<Task>

}
