@file:JvmName("StepikUpdateDateExt")

package com.jetbrains.edu.learning.stepik

import com.intellij.util.Time
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduSettings.isLoggedIn
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.StepikConnector.fillItems
import com.jetbrains.edu.learning.stepik.StepikConnector.getCourseInfo
import org.jetbrains.annotations.TestOnly
import java.util.*


fun RemoteCourse.isUpToDate(): Boolean {
  if (!isLoggedIn() || updateDate == null) {
    return true
  }

  val courseInfo = getCourseInfo(EduSettings.getInstance().user, id, isCompatible) ?: return true
  return isUpToDate(courseInfo)
}

@TestOnly
fun RemoteCourse.isUpToDate(courseFromStepik: RemoteCourse): Boolean {
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

  return isAdditionalMaterialsUpToDate(courseFromStepik)
         && sections.all { it.isUpToDate(sectionsFromServer[it.id]) }
         && lessons.all {it.isUpToDate(lessonsFromServer[it.id])}
}

fun Section.isUpToDate(sectionFromStepik: Section?): Boolean {
  if (sectionFromStepik == null) {
    return false
  }
  if (id == 0 || !isLoggedIn() || sectionFromStepik.updateDate == null || updateDate == null) {
    return true
  }

  val lessonsFromStepikById = sectionFromStepik.lessons.associateBy { it.id }
  return !sectionFromStepik.updateDate.isSignificantlyAfter(updateDate)
         && sectionFromStepik.lessons.size == lessons.size
         && lessons.all { it.isUpToDate(lessonsFromStepikById[it.id]) }
}


fun Lesson.isUpToDate(lessonFromStepik: Lesson?): Boolean {
  if (lessonFromStepik == null) {
    return false
  }

  if (id == 0 || !isLoggedIn() || lessonFromStepik.updateDate == null || updateDate == null) {
    return true
  }

  val lessonsFromServer = lessonFromStepik.taskList.associateBy { it.id }
  return !lessonFromStepik.updateDate.isSignificantlyAfter(updateDate)
         && taskList.size == lessonFromStepik.taskList.size
         && taskList.all { it.isUpToDate(lessonsFromServer[it.id]) }

}

fun Task.isUpToDate(tasksFromServer: Task?): Boolean {
  if (tasksFromServer == null) {
    return false
  }
  if (id == 0 || !isLoggedIn() || tasksFromServer.updateDate == null || updateDate == null) {
    return true
  }

  return !tasksFromServer.updateDate.isSignificantlyAfter(updateDate)
}

fun RemoteCourse.setUpdated() {
  val courseInfo = getCourseInfo(EduSettings.getInstance().user, id, isCompatible) ?: return
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

fun Date.isSignificantlyAfter(otherDate: Date): Boolean {
  val diff = time - otherDate.time
  return diff > Time.MINUTE
}

private fun RemoteCourse.isAdditionalMaterialsUpToDate(courseFromStepik: RemoteCourse): Boolean {
  val additionalLesson = courseFromStepik.getLessons(true).singleOrNull { it.isAdditional } ?: return true
  return !additionalLesson.updateDate.isSignificantlyAfter(additionalMaterialsUpdateDate)
}

private fun RemoteCourse.hasNewOrRemovedSections(courseFromStepik: RemoteCourse): Boolean {
  return courseFromStepik.sections.size != sections.size
}

private fun RemoteCourse.hasNewOrRemovedTopLevelLessons(courseFromStepik: RemoteCourse): Boolean {
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
