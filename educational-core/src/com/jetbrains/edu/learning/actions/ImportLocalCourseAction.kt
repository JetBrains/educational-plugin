package com.jetbrains.edu.learning.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.statistics.EduUsagesCollector

open class ImportLocalCourseAction(text: String = "Import Local Course") : DumbAwareAction(text) {
  override fun actionPerformed(e: AnActionEvent) {
    FileChooser.chooseFile(LocalCourseFileChooser, null, importLocation()) { file ->
      val fileName = file.path
      val course = EduUtils.getLocalCourse(fileName)
      when {
        course == null -> showInvalidCourseDialog()
        course.configurator == null -> showUnsupportedCourseDialog(course)
        else -> {
          saveLastImportLocation(file)
          initCourse(course)
          EduUsagesCollector.courseArchiveImported()
          JoinCourseDialog(course).show()
        }
      }
    }
  }

  protected open fun initCourse(course: Course) {
    course.isFromZip = true
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

    @JvmStatic
    fun showUnsupportedCourseDialog(course: Course) {
      val courseType = course.courseType
      val type = when (courseType) {
        EduNames.PYCHARM -> course.languageById?.displayName
        EduNames.ANDROID -> courseType
        else -> null
      }
      val message = if (type != null) {
        "$type courses are not supported"
      } else {
        """Selected "${course.name}" course is unsupported"""
      }

      Messages.showErrorDialog(message, IMPORT_ERROR_DIALOG_TITLE)
    }
  }
}
