package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchive
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.ProjectGenerator
import com.jetbrains.edu.learning.newproject.ui.CoursePanel
import java.io.IOException
import javax.swing.JComponent

class CCCreateCoursePreviewDialog(
        private val myProject: Project,
        private val myModule: Module,
        private val myCourse: Course,
        private val myConfigurator: EduConfigurator<*>
) : DialogWrapper(true) {

  private val myPanel: CoursePanel = CoursePanel(true, false).apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  init {
    title = "Course Preview"
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
      val course = ProjectGenerator.getLocalCourse(archivePath)
      if (course != null) {
        val lastProjectCreationLocation = RecentProjectsManager.getInstance().lastProjectCreationLocation
        try {
          val location = FileUtil.createTempDirectory(PREVIEW_FOLDER_PREFIX, null)
          val settings = myPanel.projectSettings
          myConfigurator.getEduCourseProjectGenerator(course)?.createCourseProject(location.absolutePath, settings)
          close(OK_EXIT_CODE)
        } catch (e: IOException) {
          LOG.error("Failed to create tmp dir for course preview", e)
          showErrorMessage()
        } finally {
          RecentProjectsManager.getInstance().lastProjectCreationLocation = lastProjectCreationLocation
        }
      }
    } else {
      showErrorMessage()
    }
  }

  private fun showErrorMessage() = updateErrorInfo(listOf(ValidationInfo("Can not create course preview")))

  companion object {
    private val LOG: Logger = Logger.getInstance(CCCreateCoursePreviewDialog::class.java)

    private const val WIDTH: Int = 370
    private const val HEIGHT: Int = 330

    private val PREVIEW_FOLDER_PREFIX: String = "course_preview"
  }
}
