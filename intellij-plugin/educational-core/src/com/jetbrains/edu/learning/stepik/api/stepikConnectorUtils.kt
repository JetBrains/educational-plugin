@file:JvmName("StepikConnectorUtils")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl

fun loadAndFillLessonAdditionalInfo(lesson: Lesson, course: Course? = null) {
  val attachmentLink = "${getStepikUrl()}/media/attachments/lesson/${lesson.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = StepikConnector.getInstance().loadAttachment(attachmentLink) ?: return
  val lessonInfo = StepikConnector.getInstance().objectMapper.readValue(infoText, LessonAdditionalInfo::class.java)

  lesson.customPresentableName = lessonInfo.customName
  lessonInfo.tasksInfo.forEach { (id, task) ->
    lesson.getTask(id)?.apply {
      name = task.name
      customPresentableName = task.customName
      taskFiles = task.taskFiles.associateBy(TaskFile::name) { it }
    }
  }
}

fun loadAndFillAdditionalCourseInfo(course: Course, attachmentLink: String? = null) {
  val link = attachmentLink ?: "${getStepikUrl()}/media/attachments/course/${course.id}/${StepikNames.ADDITIONAL_INFO}"
  val infoText = StepikConnector.getInstance().loadAttachment(link).takeIf { it != "null" } ?: return
  val courseInfo = StepikConnector.getInstance().objectMapper.readValue(infoText, CourseAdditionalInfo::class.java)

  course.additionalFiles = courseInfo.additionalFiles
  course.solutionsHidden = courseInfo.solutionsHidden
}