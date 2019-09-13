package com.jetbrains.edu.cpp

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectForFile
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.course

class CppRunFileNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {
  override fun getKey(): Key<EditorNotificationPanel> = KEY

  // since EDU 191 should overload [EditorNotifications.Provider.createNotificationPanel(VirtualFile, FileEditor, Project)]
  private fun createNotificationPanelByProject(file: VirtualFile, project: Project): EditorNotificationPanel? =
    if (needSetNotification(file, project))
      EditorNotificationPanel().also {
        it.setText("This file is intended for the student.")
        it.createActionLabel("Hide") {
          PropertiesComponent.getInstance().setValue(HIDE_NOTIFICATIONS_KEY, true)
          EditorNotifications.updateAll()
        }
      }
    else
      null

  // BACKCOMPAT: 2018.3 overload the super method with given project only in next versions
  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? =
    guessProjectForFile(file)?.let { createNotificationPanelByProject(file, it) }

  private fun needSetNotification(file: VirtualFile, project: Project): Boolean {
    if (PropertiesComponent.getInstance().isTrueValue(HIDE_NOTIFICATIONS_KEY)) {
      return false
    }

    val course = project.course ?: return false
    if (course.courseMode != CCUtils.COURSE_MODE) {
      // not a CCMode course
      return false
    }

    if (!FileUtil.namesEqual(file.name, CppCourseBuilder.EDU_RUN_CPP)) {
      // not a 'run.cpp' file
      return false
    }

    if (EduUtils.getTaskFile(project, file) == null) {
      // not a task file at all
      return false
    }

    return true
  }

  companion object {
    private val KEY: Key<EditorNotificationPanel> = Key.create("RunFileNotificationProvider")

    private const val HIDE_NOTIFICATIONS_KEY = "Edu.Cpp.CppRunFileNotificationProvider.hideNotifications"
  }
}