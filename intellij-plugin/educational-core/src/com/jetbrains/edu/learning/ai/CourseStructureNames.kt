package com.jetbrains.edu.learning.ai

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.core.format.domain.*

data class CourseStructureNames(
  val taskNames: Map<TaskEduId, TaskName>,
  val lessonNames: Map<LessonEduId, LessonName>,
  @JsonInclude(NON_EMPTY)
  val sectionNames: Map<SectionEduId, SectionName> = emptyMap()
) {
  fun deserialize(): String = jacksonObjectMapper().writeValueAsString(this)

  companion object {
    fun String.serializeToCourseStructureTranslation(): CourseStructureNames =
      jacksonObjectMapper().readValue(this, CourseStructureNames::class.java)

    fun CourseStructureNames.getTranslatedName(item: StudyItem): StudyItemName? = when (item) {
      is Task -> taskNames[TaskEduId(item.id)]
      is Lesson -> lessonNames[LessonEduId(item.id)]
      is Section -> sectionNames[SectionEduId(item.id)]
      else -> null
    }
  }
}
