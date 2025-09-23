package com.jetbrains.edu.learning.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle

class StudentInIdeFeedbackDialog(
  project: Project,
  val task: Task
) : InIdeFeedbackDialog<JbAcademyStudentFeedbackSystemInfoData>(true, project) {

  override fun computeSystemInfoDataInternal(): JbAcademyStudentFeedbackSystemInfoData {
    return JbAcademyStudentFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      CourseFeedbackInfoData.from(task.course),
      task.pathInCourse
    )
  }

  init {
    init()
  }

  override fun showJbAcademyFeedbackSystemInfoDialog(project: Project?, systemInfoData: JbAcademyStudentFeedbackSystemInfoData) {
      showSystemInfoDialog(project, systemInfoData) {
        row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
          label(systemInfoData.courseFeedbackInfoData.courseId.toString())
        }
        row(EduCoreBundle.message("ui.feedback.dialog.system.info.task")) {
          label(systemInfoData.taskPath)
        }
      }
    }
}
