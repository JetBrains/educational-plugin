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
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.setCourseLanguageEnvironment
import java.io.BufferedReader
import java.io.IOException
import java.net.URL

private val LOG = Logger.getInstance(StepikConnector::class.java.name)

fun getAvailableCourses(coursesList: CoursesList): List<EduCourse> {
  coursesList.courses.forEach { info ->
    setCourseLanguageEnvironment(info)
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
        markStepAsViewed(task.lesson.id, task.id)
      }
    })
}

private fun markStepAsViewed(lessonId: Int, stepId: Int) {
  if (lessonId == 0 || stepId == 0) {
    return
  }
  val unit = StepikConnector.getInstance().getLessonUnit(lessonId)
  val assignmentsIds = unit?.assignments
  if (assignmentsIds == null || assignmentsIds.isEmpty()) {
    LOG.warn("No assignment ids in unit ${unit?.id}")
    return
  }
  val assignments = StepikConnector.getInstance().getAssignments(assignmentsIds)
  assignments
    .filter { it.step == stepId }
    .forEach { StepikConnector.getInstance().postView(it.id, stepId) }
}

fun loadAttachment(course: Course, lesson: Lesson?) : List<TaskFile> {
  val id = lesson?.id ?: course.id
  val lessonOrCourse = if (lesson != null) "lesson" else "course"
  val attachmentLink = StepikNames.STEPIK_URL + "/media/attachments/" + lessonOrCourse + "/" + id + "/" + StepikNames.ADDITIONAL_FILES
  return loadAttachment(attachmentLink)
}

fun loadAttachment(attachmentLink: String) : List<TaskFile> {
  return try {
    val attachmentUrl = URL(attachmentLink)
    val conn = attachmentUrl.openConnection()

    val additionalInfoText = conn.getInputStream().bufferedReader().use(BufferedReader::readText)
    val additionalInfo = HyperskillConnector.getInstance().objectMapper.readValue(additionalInfoText, AdditionalInfo::class.java)
    additionalInfo.additionalFiles
  }
  catch (e: IOException) {
    LOG.info("No attachments found $attachmentLink")
    mutableListOf()
  }
}