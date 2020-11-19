package com.jetbrains.edu.learning.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

open class ImportLocalCourseAction(text: String = EduCoreBundle.message("course.dialog.open.course.from.disk")) : DumbAwareAction(text) {
  override fun actionPerformed(e: AnActionEvent) {
    FileChooser.chooseFile(LocalCourseFileChooser, null, importLocation()) { file ->
      val fileName = file.path
      var course = EduUtils.getLocalCourse(fileName)
      if (course == null) {
        showInvalidCourseDialog()
      }
      else {
        saveLastImportLocation(file)
        course = initCourse(course)
        EduCounterUsageCollector.importCourseArchive()
        JoinCourseDialog(course).show()
      }
    }
  }

  protected open fun initCourse(course: Course): Course {
    return course
  }

  companion object {
    private const val LAST_IMPORT_LOCATION = "Edu.LastImportLocation"
    private const val IMPORT_ERROR_DIALOG_TITLE = "Failed to Add Local Course"

    @JvmStatic
    fun importLocation(): VirtualFile? {
      val defaultDir = VfsUtil.getUserHomeDir()
      val lastImportLocation = PropertiesComponent.getInstance().getValue(LAST_IMPORT_LOCATION) ?: return defaultDir
      return LocalFileSystem.getInstance().findFileByPath(lastImportLocation) ?: defaultDir
    }

    @JvmStatic
    fun saveLastImportLocation(file: VirtualFile) {
      val location = if (!file.isDirectory) file.parent ?: return else file
      PropertiesComponent.getInstance().setValue(LAST_IMPORT_LOCATION, location.path)
    }

    @JvmStatic
    fun showInvalidCourseDialog() {
      Messages.showErrorDialog("Selected archive doesn't contain a valid course", IMPORT_ERROR_DIALOG_TITLE)
    }
  }
}
