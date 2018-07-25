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
    if (task.stepId == 0) return
    task.stepikChangeStatus = UP_TO_DATE
  }

  @JvmStatic
  fun contentChanged(studyItem: StudyItem) {
    when (studyItem) {
      is Course -> studyItem.stepikChangeStatus = if (studyItem.stepikChangeStatus == INFO) INFO_AND_CONTENT else CONTENT
      is Section -> studyItem.stepikChangeStatus = if (studyItem.stepikChangeStatus == INFO) INFO_AND_CONTENT else CONTENT
      is Lesson -> studyItem.stepikChangeStatus = if (studyItem.stepikChangeStatus == INFO) INFO_AND_CONTENT else CONTENT
    }
  }

  @JvmStatic
  fun infoChanged(item: StudyItem?) {
    if (item?.id == 0) return

    when (item) {
      is Course -> item.stepikChangeStatus = if (item.stepikChangeStatus == CONTENT) INFO_AND_CONTENT else INFO
      is Section -> item.stepikChangeStatus = if (item.stepikChangeStatus == CONTENT) INFO_AND_CONTENT else INFO
      is Lesson -> item.stepikChangeStatus = if (item.stepikChangeStatus == CONTENT) INFO_AND_CONTENT else INFO
      is Task -> changed(item)
    }
  }
}