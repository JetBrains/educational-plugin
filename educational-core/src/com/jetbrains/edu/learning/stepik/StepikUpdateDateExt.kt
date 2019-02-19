@file:JvmName("StepikUpdateDateExt")

package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.util.Time
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.api.StepikConnector.getCourseInfo
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader.fillItems
import java.util.*


fun EduCourse.isUpToDate(): Boolean {
  // disable update for courses with framework lessons as now it's unsupported
  if (lessons.plus(sections.flatMap { it.lessons }).any { it is FrameworkLesson }) {
    return true
  }

  if (updateDate == null || course.id == 0) {
    return true
  }

  val courseInfo = getCourseInfo(id, isCompatible) ?: return true
  return isUpToDate(courseInfo)
}

@VisibleForTesting
fun EduCourse.isUpToDate(courseFromStepik: EduCourse): Boolean {
  val dateFromServer = courseFromStepik.updateDate ?: return true

  if (dateFromServer.isSignificantlyAfter(updateDate)) {
    return false
  }

  if (!isUnitTestMode) {
    fillItems(courseFromStepik)
  }

  if (hasNewOrRemovedSections(courseFromStepik) || hasNewOrRemovedTopLevelLessons(courseFromStepik)) {
    return false
  }

  val sectionsFromServer = courseFromStepik.sections.associateBy { it.id }
  val lessonsFromServer = courseFromStepik.lessons.associateBy { it.id }

  return sections.all { it.isUpToDate(sectionsFromServer[it.id]) }
         && lessons.all {it.isUpToDate(lessonsFromServer[it.id])}
}

private fun Section.isUpToDate(sectionFromStepik: Section?): Boolean {
  if (sectionFromStepik == null) {
    return false
  }
  if (id == 0 || sectionFromStepik.updateDate == null || updateDate == null) {
    return true
  }

  val lessonsFromStepikById = sectionFromStepik.lessons.associateBy { it.id }
  return !sectionFromStepik.updateDate.isSignificantlyAfter(updateDate)
         && sectionFromStepik.lessons.size == lessons.size
         && lessons.all { it.isUpToDate(lessonsFromStepikById[it.id]) }
}


private fun Lesson.isUpToDate(lessonFromStepik: Lesson?): Boolean {
  if (lessonFromStepik == null) {
    return false
  }

  if (id == 0 || lessonFromStepik.updateDate == null || updateDate == null) {
    return true
  }

  val lessonsFromServer = lessonFromStepik.taskList.associateBy { it.id }
  return !lessonFromStepik.updateDate.isSignificantlyAfter(updateDate)
         && taskList.size == lessonFromStepik.taskList.size
         && taskList.all { it.isUpToDate(lessonsFromServer[it.id]) }

}

private fun Task.isUpToDate(tasksFromServer: Task?): Boolean {
  if (tasksFromServer == null) {
    return false
  }
  if (id == 0 || tasksFromServer.updateDate == null || updateDate == null) {
    return true
  }

  return !tasksFromServer.updateDate.isSignificantlyAfter(updateDate)
}

fun EduCourse.setUpdated() {
  val courseInfo = getCourseInfo(id, isCompatible) ?: return
  fillItems(courseInfo)

  updateDate = courseInfo.updateDate

  val lessonsById = courseInfo.lessons.associateBy { it.id }
  lessons.forEach {
    val lessonFromServer = lessonsById[it.id] ?: error("Lesson with id ${it.id} not found")
    it.setUpdated(lessonFromServer)
  }

  val sectionsById = courseInfo.sections.associateBy { it.id }
  sections.forEach {
    val sectionFromServer = sectionsById[it.id] ?: error("Section with id ${it.id} not found")
    it.setUpdated(sectionFromServer)
  }
}

internal fun Date.isSignificantlyAfter(otherDate: Date): Boolean {
  val diff = time - otherDate.time
  return diff > Time.MINUTE
}

private fun EduCourse.hasNewOrRemovedSections(courseFromStepik: EduCourse): Boolean {
  return courseFromStepik.sections.size != sections.size
}

private fun EduCourse.hasNewOrRemovedTopLevelLessons(courseFromStepik: EduCourse): Boolean {
  if (!hasTopLevelLessons) {
    return false
  }

  return courseFromStepik.lessons.size != lessons.size
}

private fun Section.setUpdated(sectionFromStepik: Section) {
  updateDate = sectionFromStepik.updateDate
  val lessonsById = sectionFromStepik.lessons.associateBy { it.id }
  lessons.forEach {
    val lessonFromServer = lessonsById[it.id] ?: error("Lesson with id ${it.id} not found")
    it.setUpdated(lessonFromServer)
  }
}

private fun Lesson.setUpdated(lessonFromServer: Lesson) {
  updateDate = lessonFromServer.updateDate
  val tasksById = lessonFromServer.taskList.associateBy { it.id }
  taskList.forEach {
    val taskFromServer = tasksById[it.stepId] ?: error("Task with id ${it.stepId} not found")
    it.updateDate = taskFromServer.updateDate
  }
}
