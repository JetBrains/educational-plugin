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
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.util.concurrent.atomic.AtomicBoolean

class UpdateCourseNotificationProvider(val project: Project) :
  EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {

  private val VirtualFile.isTaskFile: Boolean
    get() = EduUtils.getTaskFile(project, this) != null
  private var isUpdateRunning: AtomicBoolean = AtomicBoolean(false)

  companion object {
    @VisibleForTesting
    val KEY: Key<EditorNotificationPanel> = Key.create("Edu.updateCourse")
  }

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
    if (!EduUtils.isStudentProject(project)) {
      return null
    }
    val course = project.course as? EduCourse ?: return null
    if (course.isRemote && !course.isUpToDate && file.isTaskFile) {
      val panel = EditorNotificationPanel()
      panel.setText(EduCoreBundle.message("stepik.update.course.notification"))
      panel.createActionLabel(EduCoreBundle.message("action.update.content")) {
        if (isUpdateRunning.get()) return@createActionLabel
        ProgressManager.getInstance().run(
          object : com.intellij.openapi.progress.Task.Backgroundable(project, EduCoreBundle.message("action.update.content")) {
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
}