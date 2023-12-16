package com.jetbrains.edu.learning.uIfeedback

import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialogWithEmail
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.*
import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import com.intellij.platform.feedback.impl.notification.ThanksForFeedbackNotification
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.annotations.NonNls

@Suppress("UnstableApiUsage")
class InIdeFeedbackDialog(
  private val project: Project,
  val task: Task,
  forTest: Boolean
) : BlockBasedFeedbackDialogWithEmail<JbAcademyFeedbackSystemInfoData>(project, forTest) {

  override val myFeedbackJsonVersion: Int = super.myFeedbackJsonVersion + 1

  override val myBlocks: List<FeedbackBlock> = listOf(
    TopLabelBlock(EduCoreBundle.message("ui.feedback.dialog.title")),
    DescriptionBlock(EduCoreBundle.message("ui.feedback.dialog.description")),
    RatingBlock(EduCoreBundle.message("ui.feedback.dialog.rating.label"), "rate_impression"),
    TextAreaBlock(EduCoreBundle.message("ui.feedback.dialog.textarea.label"), "textarea_experience")
  )

  override val myFeedbackReportId: String = "academy_feedback"

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showJbAcademyFeedbackSystemInfoDialog(project, mySystemInfoData)
  }

  override val mySystemInfoData: JbAcademyFeedbackSystemInfoData by lazy {
    createJbAcademyFeedbackSystemInfoData(task)
  }

  override val myTitle: String = EduCoreBundle.message("ui.feedback.dialog.top.title")
  override val zendeskFeedbackType: String = JBA_IN_IDE_FEEDBACK
  override val zendeskTicketTitle: String = JBA_IN_IDE_FEEDBACK

  init {
    init()
  }

  public override fun showThanksNotification() {
    ThanksForFeedbackNotification(description = EduCoreBundle.message("ui.feedback.thanks.notification.content")).notify(project)
  }

  companion object {
    @NonNls
    private const val JBA_IN_IDE_FEEDBACK = "JBAcademy in-IDE Feedback"
  }
}

@Suppress("UnstableApiUsage")
data class JbAcademyFeedbackSystemInfoData(
  private val task: Task,
  val commonSystemInfo: CommonFeedbackSystemData
) : SystemDataJsonSerializable {
  private val course: Course = task.course
  val taskPath: String = task.getPathInCourse()
  val isStudent: Boolean = course.isStudy
  val courseType: String = course.name
  val courseId: Int = course.id
  val courseName: String = course.name

  override fun toString(): String {
    return buildString {
      appendLinesWithName(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode"))
      appendLinesWithName(
        if (isStudent) EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.student")
        else EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.teacher")
      )
      appendLinesWithName(EduCoreBundle.message("ui.feedback.dialog.system.info.course.type"))
      appendLinesWithName(courseType)
      appendLinesWithName(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id"))
      appendLinesWithName(courseId.toString())
      appendLinesWithName(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name"))
      appendLinesWithName(courseName)
      appendLinesWithName(EduCoreBundle.message("ui.feedback.dialog.system.info.task"))
      appendLinesWithName(taskPath)
      commonSystemInfo.toString()
    }
  }
  
  private fun StringBuilder.appendLinesWithName(lineName: String) {
    appendLine(lineName)
    appendLine()
  }

  override fun serializeToJson(json: Json): JsonElement {
    return json.encodeToJsonElement(this)
  }
}

@Suppress("UnstableApiUsage")
private fun showJbAcademyFeedbackSystemInfoDialog(
  project: Project,
  systemInfoData: JbAcademyFeedbackSystemInfoData
) = showFeedbackSystemInfoDialog(project, systemInfoData.commonSystemInfo) {
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode")) {
    label(
      if (systemInfoData.isStudent) EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.student")
      else EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.teacher")
    )
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.type")) {
    label(systemInfoData.courseType)
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
    label(systemInfoData.courseId.toString())
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
    label(systemInfoData.courseName)
  }
  row(EduCoreBundle.message("ui.feedback.dialog.system.info.task")) {
    label(systemInfoData.taskPath)
  }
}

@Suppress("UnstableApiUsage")
private fun createJbAcademyFeedbackSystemInfoData(task: Task): JbAcademyFeedbackSystemInfoData {
  return JbAcademyFeedbackSystemInfoData(
    task,
    CommonFeedbackSystemData.getCurrentData()
  )
}