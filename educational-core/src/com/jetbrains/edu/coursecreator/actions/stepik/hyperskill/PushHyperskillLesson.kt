package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.coursecreator.CCUtils.addGluingSlash
import com.jetbrains.edu.coursecreator.CCUtils.checkIfAuthorizedToStepik
import com.jetbrains.edu.coursecreator.CCUtils.lessonFromDir
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.EduExperimentalFeatures.CC_HYPERSKILL
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveRemoteInfo
import org.jetbrains.annotations.VisibleForTesting

class PushHyperskillLesson : DumbAwareAction(addGluingSlash(updateTitleText, uploadTitleText)) {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    if (!isFeatureEnabled(CC_HYPERSKILL)) return
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return
    val lesson = getLesson(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY), project, course) ?: return
    if (lesson.id > 0) {
      e.presentation.text = updateTitleText
    }
    else {
      e.presentation.text = uploadTitleText
    }
    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    if (!checkIfAuthorizedToStepik(project, e.presentation.text)) return
    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return
    val lesson = getLesson(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY), project, course) ?: return
    ProgressManager.getInstance().run(
      object : Task.Modal(project, message("action.push.custom.lesson.uploading", HYPERSKILL), true) {
        override fun run(indicator: ProgressIndicator) {
          indicator.text = message("action.push.custom.lesson.uploading.to", HYPERSKILL, getStepikUrl())
          doPush(lesson, project)
          saveRemoteInfo(lesson)
        }
      })
  }

  companion object {
    private val LOG = Logger.getInstance(PushHyperskillLesson::class.java)
    val updateTitleText: @NlsActions.ActionText String
      get() = message("item.update.on.0.lesson.custom.title", StepikNames.STEPIK, HYPERSKILL)
    val uploadTitleText: @NlsActions.ActionText String
      get() = message("item.upload.to.0.lesson.custom.title", StepikNames.STEPIK, HYPERSKILL)

    private fun getLesson(selectedFiles: Array<VirtualFile>?, project: Project, course: Course): Lesson? {
      if (course.courseMode != CourseMode.EDUCATOR) return null
      if (selectedFiles == null || selectedFiles.size != 1) {
        return null
      }
      val lessonDir = selectedFiles[0]
      return if (!lessonDir.isDirectory) {
        null
      }
      else lessonFromDir(course, lessonDir, project)
    }

    @VisibleForTesting
    fun doPush(lesson: Lesson, project: Project) {
      val notification = if (lesson.id > 0) message("action.push.custom.lesson.updated", HYPERSKILL)
      else message("action.push.custom.lesson.uploaded", HYPERSKILL)
      val success = if (lesson.id > 0) {
        CCStepikConnector.updateLesson(project, lesson, true, -1)
      }
      else {
        CCStepikConnector.postLesson(project, lesson, lesson.index, -1)
      }
      if (success) {
        showNotification(project, notification, CCStepikConnector.openOnStepikAction("/lesson/" + lesson.id))
      }
      else {
        LOG.error("Failed to update Hyperskill lesson")
      }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}