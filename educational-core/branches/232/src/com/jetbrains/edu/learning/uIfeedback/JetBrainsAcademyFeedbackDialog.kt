package com.jetbrains.edu.learning.uIfeedback

import com.intellij.feedback.common.dialog.BlockBasedFeedbackDialogWithEmail
import com.intellij.feedback.common.dialog.CommonFeedbackSystemInfoData
import com.intellij.feedback.common.dialog.JsonSerializable
import com.intellij.feedback.common.dialog.showFeedbackSystemInfoDialog
import com.intellij.feedback.common.dialog.uiBlocks.*
import com.intellij.feedback.common.notification.ThanksForFeedbackNotification
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

class JetBrainsAcademyFeedbackDialog(
  private val project: Project,
  private val course: Course,
  val task: Task,
  forTest: Boolean
) : BlockBasedFeedbackDialogWithEmail<JbAcademyFeedbackSystemInfoData>(project, forTest) {

  //should we override? or no sense if outside intellij. or should ve introduce our own versioning
  override val myFeedbackJsonVersion: Int = super.myFeedbackJsonVersion + 1
  override val myBlocks: List<FeedbackBlock>
    get() = listOf(
  TopLabelBlock(EduCoreBundle.message("ui.feedback.dialog.title")),
  DescriptionBlock(EduCoreBundle.message("ui.feedback.dialog.description")),
  RatingBlock(EduCoreBundle.message("ui.feedback.dialog.rating.label"), "rate_impression"),
  TextAreaBlock(EduCoreBundle.message("ui.feedback.dialog.textarea.label"), "textarea_experience")
  )

  override val myFeedbackReportId: String
    get() = "academy_feedback"

  override val myShowFeedbackSystemInfoDialog: () -> Unit = { showJbAcademyFeedbackSystemInfoDialog(project, mySystemInfoData) }

  override val mySystemInfoData: JbAcademyFeedbackSystemInfoData by lazy {
    createJbAcademyFeedbackSystemInfoData(course, task.getPathInCourse())
  }

  override val myTitle: String
    get() = EduCoreBundle.message("ui.feedback.dialog.top.title")
  override val zendeskFeedbackType: String
    get() = "JBAcademy in-IDE Feedback"
  override val zendeskTicketTitle: String
    get() = "JBAcademy in-IDE Feedback"

  override fun showThanksNotification() {
    ThanksForFeedbackNotification(description = EduCoreBundle.message("ui.feedback.thanks.notification.content")).notify(project)
  }

  override fun sendFeedbackData() {
    //super.sendFeedbackData()
    return
  }
}

//use @Serializable
data class JbAcademyFeedbackSystemInfoData(
  val isStudent: Boolean,
  val courseType: String,
  val courseId: Int,
  val courseName: String,
  val taskPath: String,
  val commonSystemInfo: CommonFeedbackSystemInfoData
): JsonSerializable {
  override fun toString(): String {
    return buildString {
      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode"))
      appendLine()
      appendLine(if (isStudent) EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.student") else EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.teacher"))
      appendLine()

      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.type"))
      appendLine()
      appendLine(courseType)
      appendLine()

      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id"))
      appendLine()
      appendLine(courseId)
      appendLine()

      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name"))
      appendLine()
      appendLine(courseName)
      appendLine()

      appendLine(EduCoreBundle.message("ui.feedback.dialog.system.info.task"))
      appendLine()
      appendLine(taskPath)
      appendLine()
      commonSystemInfo.toString()
    }
  }

  override fun serializeToJson(json: Json): JsonElement {
    return json.encodeToJsonElement(this)
  }
}

private fun showJbAcademyFeedbackSystemInfoDialog(
  project: Project,
  systemInfoData: JbAcademyFeedbackSystemInfoData
) = showFeedbackSystemInfoDialog(project, systemInfoData.commonSystemInfo) {
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode")) {
    label(
      if (systemInfoData.isStudent) EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.student") else EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.teacher")
    )
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.type")) {
    label(
      systemInfoData.courseType
    )
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
    label(
      systemInfoData.courseId.toString()
    )
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
    label(
      systemInfoData.courseName
    )
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.task")) {
    label(
      systemInfoData.taskPath
    )
  }
}

private fun createJbAcademyFeedbackSystemInfoData(course: Course, taskPath: String): JbAcademyFeedbackSystemInfoData {
  return JbAcademyFeedbackSystemInfoData(course.isStudy, course.itemType, course.id, course.name, taskPath, CommonFeedbackSystemInfoData.getCurrentData())
}