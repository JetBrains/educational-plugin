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

fun loadAndFillLessonAdditionalInfo(lesson: Lesson) {
  val attachmentLink = "${StepikNames.STEPIK_URL}/media/attachments/lesson/${lesson.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = loadAttachment(attachmentLink) ?: return
  val lessonInfo = HyperskillConnector.getInstance().objectMapper.readValue(infoText, AdditionalLessonInfo::class.java)

  lesson.customPresentableName = lessonInfo.customName
  lessonInfo.taskFiles.forEach { (taskId, taskFiles) ->
    lesson.getTask(taskId)?.taskFiles = taskFiles.associateBy(TaskFile::getName) { it }
  }
}

fun loadAndFillAdditionalCourseInfo(course: Course, attachmentLink: String? = null) {
  val link = attachmentLink ?: "${StepikNames.STEPIK_URL}/media/attachments/course/${course.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = loadAttachment(link) ?: return
  val courseInfo = HyperskillConnector.getInstance().objectMapper.readValue(infoText, AdditionalCourseInfo::class.java)

  course.additionalFiles = courseInfo.additionalFiles
  course.solutionsHidden = courseInfo.solutionsHidden
}

private fun loadAttachment(attachmentLink: String): String? {
  try {
    val conn = URL(attachmentLink).openConnection()
    return conn.getInputStream().bufferedReader().use(BufferedReader::readText)
  }
  catch (e: IOException) {
    LOG.info("No attachments found $attachmentLink")
  }
  return null
}