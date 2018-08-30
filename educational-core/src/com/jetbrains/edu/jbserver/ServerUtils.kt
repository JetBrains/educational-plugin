package com.jetbrains.edu.jbserver

import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem


fun StudyItem.isChanged()
  = stepikChangeStatus != StepikChangeStatus.UP_TO_DATE


fun <T> MutableList<T>.mapInplace (mutator: (T) -> T) {
  val iterator = this.toMutableList().listIterator()
  while (iterator.hasNext()) {
    val oldValue = iterator.next()
    val newValue = mutator(oldValue)
    iterator.set(newValue)
  }
}

fun checkUpdate(course: EduCourse) {

  if (!ServerConnector.isCourseUpdated(course)) return

  // todo : should we use other ui element here ?

  val confirmUpdate = MessageDialogBuilder.yesNo(
    "Course update available",
    "Update for course `${course.name}` is available. Please update the course, otherwise some functionality may not work."
  ).yesText("Update").noText("Continue without update")

  if (confirmUpdate.show() == Messages.YES) try {
    ServerConnector.getCourseUpdate(course)
    EduUtils.notify("Course update", "Course `${course.name}` updated successfully", NotificationType.INFORMATION)
  }
  catch (e: Exception) {
    EduUtils.notify("Course update", "Error occured while updating course `${course.name}`", NotificationType.ERROR)
  }
}