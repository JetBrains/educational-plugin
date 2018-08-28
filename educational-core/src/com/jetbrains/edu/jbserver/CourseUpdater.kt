package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.Date


class CourseUpdater(val course: EduCourse, val update: EduCourse) {

  val sections = mutableMapOf<Int, Section>()
  val lessons = mutableMapOf<Int, Lesson>()
  val tasks = mutableMapOf<Int, Task>()

  init {
    traverseCourse(course)
    traverseUpdate(update)
  }

  private fun traverseCourse(studyItem: StudyItem) {
    when(studyItem) {
      is EduCourse -> {
        studyItem.items.forEach { traverseCourse(it) }
      }
      is Section -> {
        sections[studyItem.id] = studyItem
        studyItem.items.forEach { traverseCourse(it) }
      }
      is Lesson -> {
        lessons[studyItem.id] = studyItem
        studyItem.taskList.forEach { traverseCourse(it) }
      }
      is Task -> {
        tasks[studyItem.versionId] = studyItem
      }
    }
  }

  private fun traverseUpdate(eduCourse: EduCourse) {
    eduCourse.name = course.name
    eduCourse.description = course.description
    eduCourse.languageCode = course.languageCode
    eduCourse.language = course.language
    eduCourse.items.mapInplace {
      when(it) {
        is Section -> traverseUpdate(it)
        is Lesson -> traverseUpdate(it)
        else -> throw IllegalStateException("Only section and lessons can be top-level elements")
      }
    }
  }

  private fun traverseUpdate(section: Section) = when {
    sections[section.id]?.updateDate ?: Date(0) == section.updateDate -> {
      sections[section.id]!!
    }
    section.id in sections -> {
      section.name = sections[section.id]!!.name
      section.items.mapInplace { traverseUpdate(it as Lesson) }
      section
    }
    else -> {
      ServerConnector.getSection(section.id)
    }
  }

  private fun traverseUpdate(lesson: Lesson) = when {
    lessons[lesson.id]?.updateDate ?: Date(0) == lesson.updateDate -> {
      lessons[lesson.id]!!
    }
    lesson.id in lessons -> {
      lesson.name = lessons[lesson.id]!!.name
      lesson.taskList.mapInplace { traverseUpdate(it) }
      lesson
    }
    else -> {
      ServerConnector.getLesson(lesson.id)
    }
  }

  private fun traverseUpdate(task: Task) =
    tasks[task.versionId] ?: ServerConnector.getTask(task.id)

}
