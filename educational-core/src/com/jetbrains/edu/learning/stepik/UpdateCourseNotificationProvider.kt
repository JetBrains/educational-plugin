package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.marketplace.isRemoteUpdateFormatVersionCompatible
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.marketplace.update.getUpdateInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.concurrent.atomic.AtomicBoolean

class UpdateCourseNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {

  private var isUpdateRunning: AtomicBoolean = AtomicBoolean(false)

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (!EduUtils.isStudentProject(project)) {
      return null
    }
    val course = project.course as? EduCourse ?: return null
    if ((course.isStepikRemote || course.isMarketplaceRemote) && !course.isUpToDate && file.getTaskFile(project) != null) {
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
      return panel
    }

    return null
  }

  private fun updateCourse(project: Project, course: EduCourse) {
    when (course.isMarketplace) {
      true -> {
        val updateInfo = course.getUpdateInfo() ?: return
        if (!isRemoteUpdateFormatVersionCompatible(project, updateInfo.compatibility.gte)) return
        MarketplaceCourseUpdater(project, course, updateInfo.version).updateCourse()
      }
      false -> updateCourseOnStepik(project, course)
    }
  }

  companion object {
    @VisibleForTesting
    val KEY: Key<EditorNotificationPanel> = Key.create("Edu.updateCourse")
  }
}
