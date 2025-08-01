package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.marketplace.isRemoteUpdateFormatVersionCompatible
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.marketplace.update.getUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import javax.swing.JComponent

class UpdateCourseNotificationProvider : EditorNotificationProvider, DumbAware {

  private var isUpdateRunning: AtomicBoolean = AtomicBoolean(false)

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!project.isStudentProject()) {
      return null
    }
    val course = project.course as? EduCourse ?: return null
    if (!course.isStepikRemote && !course.isMarketplaceRemote || course.isUpToDate || file.getTaskFile(project) == null) return null

    return Function {
      val panel = EditorNotificationPanel()
      panel.text = EduCoreBundle.message("update.notification")
      panel.createActionLabel(EduCoreBundle.message("update.action")) {
        if (isUpdateRunning.get()) return@createActionLabel
        ProgressManager.getInstance().run(
          object : com.intellij.openapi.progress.Task.Backgroundable(project, EduCoreBundle.message("update.content")) {
            override fun run(indicator: ProgressIndicator) {
              isUpdateRunning.set(true)
              updateCourse(project, course)
              isUpdateRunning.set(false)
            }
          })
      }
      panel
    }
  }

  private fun updateCourse(project: Project, course: EduCourse) {
    if (!course.isMarketplace) return
    val updateInfo = course.getUpdateInfo() ?: return
    if (!isRemoteUpdateFormatVersionCompatible(project, updateInfo.formatVersion)) return
    MarketplaceCourseUpdater(project, course, updateInfo.courseVersion).updateCourse()
  }
}
