package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages.showErrorDialog
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.EduCourseArchiveCreator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.encrypt.EncryptionBundle
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.awt.event.ActionEvent
import java.io.IOException
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class CCCreateCoursePreviewDialog(
  private val myProject: Project,
  private val myCourse: Course,
  private val myConfigurator: EduConfigurator<*>
) : DialogWrapper(true) {

  private val myPanel: CoursePanel = CourseArchivePanel()

  init {
    title = "Course Preview"
    setOKButtonText("Create")
    myPanel.preferredSize = JBUI.size(WIDTH, HEIGHT)
    myPanel.minimumSize = JBUI.size(WIDTH, HEIGHT)
    val courseCopy = myCourse.copy().apply {
      putUserData(IS_COURSE_PREVIEW_KEY, true)
    }
    myPanel.bindCourse(courseCopy)
    init()
    UIUtil.setBackgroundRecursively(rootPane, MAIN_BG_COLOR)
  }

  override fun createCenterPanel(): JComponent = myPanel

  override fun createActions(): Array<out Action> {
    val closeAction = object : AbstractAction(UIUtil.replaceMnemonicAmpersand("&Close")) {
      override fun actionPerformed(e: ActionEvent) {
        close()
      }
    }

    return arrayOf(closeAction)
  }

  fun close() {
    close(OK_EXIT_CODE)
  }

  override fun getStyle(): DialogStyle {
    return DialogStyle.COMPACT
  }


  private inner class CourseArchivePanel : CoursePanel(false) {
    override val joinCourseAction: (CourseInfo, CourseMode, CoursePanel) -> Unit = { _, _, _ -> createCoursePreview() }

    private val LOG: Logger = Logger.getInstance(CCCreateCoursePreviewDialog::class.java)

    override fun showError(errorState: ErrorState) { }

    private fun createCoursePreview() {
      val folder = CCUtils.getGeneratedFilesFolder(myProject)
      if (folder == null) {
        LOG.info(TMP_DIR_ERROR)
        showErrorDialog(myProject, "$TMP_DIR_ERROR Please check permissions and try again.", PREVIEW_CREATION_ERROR_TITLE)
        return
      }
      val courseName = myCourse.name
      val archiveName = if (courseName.isNullOrEmpty()) EduNames.COURSE else FileUtil.sanitizeFileName(courseName)
      val archiveLocation = "${folder.path}/$archiveName.zip"
      close(OK_EXIT_CODE)
      val errorMessage = ApplicationManager.getApplication().runWriteAction<String>(
        EduCourseArchiveCreator(myProject, archiveLocation, EncryptionBundle.message("aesKey")))

      if (errorMessage.isNullOrEmpty()) {
        val archivePath = FileUtil.join(FileUtil.toSystemDependentName(folder.path), "$archiveName.zip")
        val course = EduUtils.getLocalCourse(archivePath)
        if (course != null) {
          val lastProjectCreationLocation = RecentProjectsManager.getInstance().lastProjectCreationLocation
          try {
            val location = FileUtil.createTempDirectory(PREVIEW_FOLDER_PREFIX, null)
            val settings = myPanel.projectSettings ?: error("Project settings shouldn't be null")
            val previewProject = myConfigurator.courseBuilder.getCourseProjectGenerator(course)
              ?.doCreateCourseProject(location.absolutePath, settings)
            if (previewProject == null) {
              LOG.info(PREVIEW_PROJECT_ERROR)
              showErrorDialog(myProject, "$PREVIEW_PROJECT_ERROR Please try again.", PREVIEW_CREATION_ERROR_TITLE)
              return
            }
            PropertiesComponent.getInstance(previewProject).setValue(IS_COURSE_PREVIEW, true)
            RecentProjectsManager.getInstance().removePath(location.absolutePath)
            EduCounterUsageCollector.createCoursePreview()
          }
          catch (e: IOException) {
            LOG.info(TMP_DIR_ERROR, e)
            showErrorDialog(myProject, "$TMP_DIR_ERROR Please check permissions and try again.", PREVIEW_CREATION_ERROR_TITLE)
          }
          finally {
            RecentProjectsManager.getInstance().lastProjectCreationLocation = lastProjectCreationLocation
          }
        }
      }
      else {
        LOG.info(errorMessage)
        showErrorDialog(myProject, errorMessage, PREVIEW_CREATION_ERROR_TITLE)
      }
    }
  }

  companion object {
    private const val WIDTH: Int = 450
    private const val HEIGHT: Int = 330
    private const val PREVIEW_CREATION_ERROR_TITLE = "Failed to Create Course Preview"
    private const val TMP_DIR_ERROR = "Failed to create temp directory for course preview."
    private const val PREVIEW_PROJECT_ERROR = "Failed to create project for course preview."

    const val PREVIEW_FOLDER_PREFIX: String = "course_preview"
    const val IS_COURSE_PREVIEW: String = "Edu.IsCoursePreview"
    const val IS_LOCAL_COURSE: String = "Edu.IsLocalCourse"

    @JvmField
    val IS_COURSE_PREVIEW_KEY = Key.create<Boolean>(IS_COURSE_PREVIEW)
    val IS_LOCAL_COURSE_KEY = Key.create<Boolean>(IS_LOCAL_COURSE)
  }
}
