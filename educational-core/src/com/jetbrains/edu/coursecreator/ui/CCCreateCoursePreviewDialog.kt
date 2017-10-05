package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchive
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.core.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator
import com.jetbrains.edu.learning.newproject.ui.EduCoursePanel
import java.io.IOException
import javax.swing.JComponent

class CCCreateCoursePreviewDialog(
        private val myProject: Project,
        private val myModule: Module,
        private val myCourse: Course,
        private val myConfigurator: EduPluginConfigurator
) : DialogWrapper(true) {

  private val myPanel: EduCoursePanel = EduCoursePanel(false).apply {
    preferredSize = JBUI.size(400, 350)
    minimumSize = JBUI.size(400, 350)
  }

  init {
    title = "Create Course Preview"
    setOKButtonText("Create")
    myPanel.bindCourse(myCourse)
    init()
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun doOKAction() {
    val folder = CCUtils.getGeneratedFilesFolder(myProject, myModule)
    val courseName = myCourse.name
    val archiveName = if (courseName.isNullOrEmpty()) EduNames.COURSE else FileUtil.sanitizeFileName(courseName)
    val locationDir = folder.path
    val isSuccessful = CCCreateCourseArchive.createCourseArchive(myProject, myModule, archiveName, locationDir, false)

    if (isSuccessful) {
      val archivePath = FileUtil.join(FileUtil.toSystemDependentName(folder.path), archiveName + ".zip")
      val course = StudyProjectGenerator.getCourse(archivePath)
      if (course != null) {
        val lastProjectCreationLocation = RecentProjectsManager.getInstance().lastProjectCreationLocation
        try {
          val location = FileUtil.createTempDirectory(PREVIEW_FOLDER_PREFIX, null)
          myConfigurator.eduCourseProjectGenerator?.createProject(course, location.absolutePath)
        } catch (e: IOException) {
          LOG.error("Failed to create tmp dir for course preview", e)
          showErrorDialog()
        } finally {
          RecentProjectsManager.getInstance().lastProjectCreationLocation = lastProjectCreationLocation
        }
      }
    } else {
      showErrorDialog()
    }
    close(OK_EXIT_CODE)
  }

  private fun showErrorDialog() = Messages.showErrorDialog("Can not create course preview", "Failed to Create Course Preview")

  companion object {
    private val LOG: Logger = Logger.getInstance(CCCreateCoursePreviewDialog::class.java)

    private val PREVIEW_FOLDER_PREFIX: String = "course_preview"
  }
}
