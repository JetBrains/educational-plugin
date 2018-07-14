package com.jetbrains.edu.coursecreator.stepik

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object StepikCourseChangeHandler {
  @JvmStatic
  fun changed(placeholder: AnswerPlaceholder) {
    val taskFile = placeholder.taskFile ?: return
    changed(taskFile)
  }

  @JvmStatic
  fun changed(taskFile: TaskFile) {
    changed(taskFile.task)
  }

  @JvmStatic
  fun changed(task: Task) {
    if (task.id == 0) return
    task.stepikChangeStatus = INFO_AND_CONTENT
  }

  @JvmStatic
  fun notChanged(task: Task) {
    if (task.id == 0) return
    task.stepikChangeStatus = UP_TO_DATE
  }

  @JvmStatic
  fun infoChanged(lesson: Lesson) {
    if (lesson.id == 0) return
    lesson.stepikChangeStatus = if (lesson.stepikChangeStatus == CONTENT) INFO_AND_CONTENT else INFO
  }

  @JvmStatic
  fun contentChanged(lesson: Lesson) {
    if (lesson.id == 0) return
    lesson.stepikChangeStatus = if (lesson.stepikChangeStatus == INFO) INFO_AND_CONTENT else CONTENT
  }

  @JvmStatic
  fun infoChanged(section: Section) {
    if (section.id == 0) return
    section.stepikChangeStatus = if (section.stepikChangeStatus == CONTENT) INFO_AND_CONTENT else INFO
  }

  @JvmStatic
  fun contentChanged(section: Section) {
    if (section.id == 0) return
    section.stepikChangeStatus = if (section.stepikChangeStatus == INFO) INFO_AND_CONTENT else CONTENT
  }

  @JvmStatic
  fun infoChanged(course: Course) {
    if (course !is RemoteCourse) return
    course.stepikChangeStatus = if (course.stepikChangeStatus == CONTENT) INFO_AND_CONTENT else INFO
  }

  @JvmStatic
  fun contentChanged(course: Course) {
    if (course !is RemoteCourse) return
    course.stepikChangeStatus = if (course.stepikChangeStatus == INFO) INFO_AND_CONTENT else CONTENT
  }

  @JvmStatic
  fun contentChanged(itemContainer: ItemContainer) {
    when (itemContainer) {
      is Course -> contentChanged(itemContainer)
      is Section -> contentChanged(itemContainer)
    }
  }

  @JvmStatic
  fun infoChanged(item: StudyItem?) {
    when (item) {
      is Course -> infoChanged(item)
      is Section -> infoChanged(item)
      is Lesson -> infoChanged(item)
      is Task -> changed(item)
    }
  }
}