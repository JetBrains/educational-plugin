package com.jetbrains.edu.cpp

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
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

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? =
    if (needSetNotification(file, project))
      EditorNotificationPanel().also {
        it.setText("This file is intended for the student.")
        it.createActionLabel("Hide") {
          PropertiesComponent.getInstance().setValue(HIDE_NOTIFICATIONS_KAY, true)
          EditorNotifications.updateAll()
        }
      }
    else
      null


  private fun needSetNotification(file: VirtualFile, project: Project): Boolean {
    if (PropertiesComponent.getInstance().isTrueValue(HIDE_NOTIFICATIONS_KAY)) {
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

  // It's needed to support old ide versions
  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
    throw UnsupportedOperationException()
  }

  companion object {
    val KEY = Key.create<EditorNotificationPanel>("RunFileNotificationProvider")

    const val HIDE_NOTIFICATIONS_KAY = "Edu.Cpp.CppRunFileNotificationProvider.hideNotifications"
  }
}