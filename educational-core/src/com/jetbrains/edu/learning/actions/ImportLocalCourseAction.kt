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
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.statistics.EduUsagesCollector

open class ImportLocalCourseAction(text: String = "Import Local Course") : DumbAwareAction(text) {
  override fun actionPerformed(e: AnActionEvent) {
    FileChooser.chooseFile(LocalCourseFileChooser, null, importLocation()) { file ->
      val fileName = file.path
      val course = EduUtils.getLocalCourse(fileName)
      if (course != null) {
        saveLastImportLocation(file)
        initCourse(course)
        EduUsagesCollector.courseArchiveImported()
        JoinCourseDialog(course).show()
      }
      else {
        Messages.showErrorDialog("Selected archive doesn't contain a valid course", "Failed to Add Local Course")
      }
    }
  }

  protected open fun initCourse(course: Course) {
    course.isFromZip = true
  }

  companion object {
    private const val LAST_IMPORT_LOCATION = "Edu.LastImportLocation"

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
  }
}