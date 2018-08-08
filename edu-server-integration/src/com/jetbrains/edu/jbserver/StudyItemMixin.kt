package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task


@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(*arrayOf(
  JsonSubTypes.Type(value = Course::class, name = "course"),
  JsonSubTypes.Type(value = Section::class, name = "section"),
  JsonSubTypes.Type(value = Lesson::class, name = "lesson")
))
abstract class StudyItemMixin


@JsonIgnoreProperties(value = *arrayOf(
  "id", "last_modified", "format", "course_files", "change_notes",
  "course", "tags", "authors", "courseType", "courseMode",
  "visibility", "lessons", "sections", "upToDate", "languageById",
  "languageID", "languageVersion", "adaptive", "study", "authorFullNames",
  "languageCode", "compatibility", "index", "customPresentableName",
  "presentableName", "stepikChangeStatus", "humanLanguage"
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

}


@JsonIgnoreProperties(value = *arrayOf(
  "id", "last_modified", "description", "description_format", "format",
  "course", "units", "courseId", "position", "upToDate", "updateDate", "lessons", "index",
  "customPresentableName", "presentableName", "stepikChangeStatus"
))
@JsonTypeName("section")
abstract class SectionMixin {

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("items")
  lateinit var items: List<StudyItem>

}


@JsonIgnoreProperties(value = *arrayOf(
  "id", "last_modified", "description", "description_format", "format",
  "course", "container", "steps", "tags", "unitId", "section", "upToDate", "additional",
  "taskListForProgress", "status", "updateDate", "index", "customPresentableName",
  "presentableName", "stepikChangeStatus"
))
@JsonTypeName("lesson")
abstract class LessonMixin {

  @JsonProperty("title")
  lateinit var name: String

  @JsonProperty("items")
  lateinit var taskList: List<Task>
}


/* Note: current issues
 * 1. Where to store "id", "last_modified", "description", "description_format", "version" fields
 * 2. What to do with other properties, that we don't need (now: just ignore)
 */
