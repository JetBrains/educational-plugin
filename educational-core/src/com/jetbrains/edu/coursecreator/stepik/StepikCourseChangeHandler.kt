package com.jetbrains.edu.coursecreator.stepik

import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object StepikCourseChangeHandler {
  fun changed(placeholder: AnswerPlaceholder) {
    val taskFile = placeholder.taskFile ?: return
    changed(taskFile)
  }

  fun changed(taskFile: TaskFile) {
    changed(taskFile.task)
  }

  fun changed(task: Task) {
    if (task.id == 0) return
    task.stepikChangeStatus = StepikChangeStatus.INFO_AND_CONTENT
  }

  fun notChanged(task: Task) {
    if (task.id == 0) return
    task.stepikChangeStatus = StepikChangeStatus.UP_TO_DATE
  }

  fun infoChanged(lesson: Lesson) {
    if (lesson.id == 0) return
    lesson.stepikChangeStatus = if (lesson.stepikChangeStatus == StepikChangeStatus.CONTENT) StepikChangeStatus.INFO_AND_CONTENT else StepikChangeStatus.INFO
  }

  fun contentChanged(lesson: Lesson) {
    if (lesson.id == 0) return
    lesson.stepikChangeStatus = if (lesson.stepikChangeStatus == StepikChangeStatus.INFO) StepikChangeStatus.INFO_AND_CONTENT else StepikChangeStatus.CONTENT
  }

  fun infoChanged(section: Section) {
    if (section.id == 0) return
    section.stepikChangeStatus = if (section.stepikChangeStatus == StepikChangeStatus.CONTENT) StepikChangeStatus.INFO_AND_CONTENT else StepikChangeStatus.INFO
  }

  fun contentChanged(section: Section) {
    if (section.id == 0) return
    section.stepikChangeStatus = if (section.stepikChangeStatus == StepikChangeStatus.INFO) StepikChangeStatus.INFO_AND_CONTENT else StepikChangeStatus.CONTENT
  }

  fun infoChanged(course: Course) {
    if (course !is RemoteCourse) return
    course.stepikChangeStatus = if (course.stepikChangeStatus == StepikChangeStatus.CONTENT) StepikChangeStatus.INFO_AND_CONTENT else StepikChangeStatus.INFO
  }

  fun contentChanged(course: Course) {
    if (course !is RemoteCourse) return
    course.stepikChangeStatus = if (course.stepikChangeStatus == StepikChangeStatus.INFO) StepikChangeStatus.INFO_AND_CONTENT else StepikChangeStatus.CONTENT
  }

  fun contentChanged(itemContainer: ItemContainer) {
    if (itemContainer is Course) {
      contentChanged(itemContainer)
      return
    }

    if (itemContainer is Section) {
      contentChanged(itemContainer)
      return
    }
  }

  fun infoChanged(item: StudyItem?) {
    if (item is Course) {
      infoChanged(item)
      return
    }

    if (item is Section) {
      infoChanged(item)
      return
    }

    if (item is Lesson) {
      infoChanged(item)
      return
    }

    if (item is Task) {
      changed(item)
      return
    }
  }
}