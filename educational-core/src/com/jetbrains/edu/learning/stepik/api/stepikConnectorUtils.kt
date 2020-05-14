@file:JvmName("StepikConnectorUtils")

package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.featuredCourses
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.setCourseLanguageEnvironment
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

private val LOG = Logger.getInstance(StepikConnector::class.java.name)

fun getAvailableCourses(coursesList: CoursesList): List<EduCourse> {
  val availableCourses = coursesList.courses.filter {
    !StringUtil.isEmptyOrSpaces(it.type) && it.compatibility != CourseCompatibility.Unsupported
  }

  availableCourses.forEach { it.visibility = getVisibility(it) }
  return availableCourses
}

fun addCoursesFromStepik(courses: MutableList<EduCourse>,
                         isPublic: Boolean,
                         currentPage: Int,
                         enrolled: Boolean?,
                         minEmptyPage: AtomicInteger): Boolean {
  val coursesFromStepik = StepikConnector.getInstance().getCourses(isPublic, currentPage, enrolled)
  if (coursesFromStepik == null) {
    minEmptyPage.compareAndUpdateValue(currentPage)
    return false
  }
  courses.addAll(getAvailableCourses(coursesFromStepik))
  val hasNext = coursesFromStepik.meta.containsKey("has_next")
  if (!hasNext) {
    minEmptyPage.compareAndUpdateValue(currentPage + 1)
  }
  return hasNext
}

private fun AtomicInteger.compareAndUpdateValue(newPage: Int) {
  getAndUpdate { currentValue -> min(currentValue, newPage) }
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
        markStepAsSolved(task.lesson.id, task)
      }
    })
}

private fun markStepAsSolved(lessonId: Int, task: Task) {
  if (lessonId == 0 || task.id == 0) {
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
    .filter { it.step == task.id }
    .forEach { StepikConnector.getInstance().postView(it.id, task.id) }

  val attempt = StepikConnector.getInstance().postAttempt(task.id, true)
  val submission = attempt?.let { StepikConnector.getInstance().postSubmission(true, attempt, ArrayList(), task) }
  if (submission == null) {
    LOG.warn("Post submission failed for task ${task.name} (id=${task.id})")
  }
  saveItem(task)
}

fun loadAndFillLessonAdditionalInfo(lesson: Lesson, course: Course? = null) {
  val attachmentLink = "${StepikNames.STEPIK_URL}/media/attachments/lesson/${lesson.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = StepikConnector.getInstance().loadAttachment(attachmentLink) ?: return
  val lessonInfo = HyperskillConnector.getInstance().objectMapper.readValue(infoText, LessonAdditionalInfo::class.java)

  lesson.customPresentableName = lessonInfo.customName
  lessonInfo.tasksInfo.forEach { (id, task) ->
    lesson.getTask(id)?.apply {
      name = task.name
      customPresentableName = task.customName
      taskFiles = task.taskFiles.associateBy(TaskFile::getName) { it }
    }
  }

  if (course is HyperskillCourse && lessonInfo.additionalFiles.isNotEmpty()) {
    course.additionalFiles = lessonInfo.additionalFiles
  }
}

fun loadAndFillAdditionalCourseInfo(course: Course, attachmentLink: String? = null) {
  val link = attachmentLink ?: "${StepikNames.STEPIK_URL}/media/attachments/course/${course.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = StepikConnector.getInstance().loadAttachment(link) ?: return
  val courseInfo = HyperskillConnector.getInstance().objectMapper.readValue(infoText, CourseAdditionalInfo::class.java)

  course.additionalFiles = courseInfo.additionalFiles
  course.solutionsHidden = courseInfo.solutionsHidden
}