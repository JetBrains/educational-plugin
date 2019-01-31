@file:JvmName("StepikConnectorUtils")

package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.featuredCourses
import com.jetbrains.edu.learning.stepik.hyperskill.AdditionalInfo
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector
import com.jetbrains.edu.learning.stepik.setCourseLanguage
import java.io.BufferedReader
import java.io.IOException
import java.net.URL

private val LOG = Logger.getInstance(StepikConnector::class.java.name)

fun getAvailableCourses(coursesList: CoursesList): List<EduCourse> {
  coursesList.courses.forEach { info ->
    setCourseLanguage(info)
  }
  val availableCourses = coursesList.courses.filter {
    !StringUtil.isEmptyOrSpaces(it.type)
    && it.compatibility != CourseCompatibility.UNSUPPORTED
  }

  availableCourses.forEach { it.visibility = getVisibility(it) }
  return availableCourses
}

private fun getVisibility(course: EduCourse): CourseVisibility {
  return when {
    !course.isPublic -> CourseVisibility.PrivateVisibility
    featuredCourses.contains(course.id) -> CourseVisibility.FeaturedVisibility(featuredCourses.indexOf(course.id))
    featuredCourses.isEmpty() -> CourseVisibility.LocalVisibility
    else -> CourseVisibility.PublicVisibility
  }
}

fun postTheory(task: Task, project: Project) {
  ProgressManager.getInstance().run(
    object : Backgroundable(project, "Posting Theory to Stepik", false) {
      override fun run(progressIndicator: ProgressIndicator) {
        markStepAsViewed(task.lesson.id, task.stepId)
      }
    })
}

private fun markStepAsViewed(lessonId: Int, stepId: Int) {
  val unit = StepikConnector.getLessonUnit(lessonId)
  val assignmentsIds = unit?.assignments
  if (assignmentsIds == null || assignmentsIds.isEmpty()) {
    LOG.warn("No assignment ids in unit " + unit?.id!!)
    return
  }
  val assignments = StepikMultipleRequestsConnector.getAssignments(assignmentsIds)
  assignments
    .filter { it.step == stepId }
    .forEach { StepikConnector.postView(it.id, stepId) }
}

fun loadAttachment(course: Course, lesson: Lesson?) {
  val id = lesson?.id ?: course.id
  val lessonOrCourse = if (lesson != null) "lesson" else "course"
  val attachmentLink = StepikNames.STEPIK_URL + "/media/attachments/" + lessonOrCourse + "/" + id + "/" + StepikNames.ADDITIONAL_FILES

  try {
    val attachmentUrl = URL(attachmentLink)
    val conn = attachmentUrl.openConnection()

    val additionalInfoText = conn.getInputStream().bufferedReader().use(BufferedReader::readText)
    val additionalInfo = HyperskillConnector.objectMapper.readValue(additionalInfoText, AdditionalInfo::class.java)
    course.additionalFiles = additionalInfo.additionalFiles
  }
  catch (e: IOException) {
    LOG.info("No attachments found $attachmentLink")
  }
}