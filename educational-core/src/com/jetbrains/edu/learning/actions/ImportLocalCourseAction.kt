package com.jetbrains.edu.learning.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.LocalCourseFileChooser
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls
import java.awt.Component
import java.util.function.Supplier

open class ImportLocalCourseAction(
  text: Supplier<@ActionText String> = EduCoreBundle.lazyMessage("course.dialog.open.course.from.disk.text")
) : DumbAwareAction(text, EduCoreBundle.lazyMessage("course.dialog.open.course.from.disk.description"), null) {
  override fun actionPerformed(e: AnActionEvent) {
    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    FileChooser.chooseFile(LocalCourseFileChooser, null, importLocation()) { file ->
      val fileName = file.path
      val course = EduUtils.getLocalCourse(fileName)
      if (course == null) {
        showInvalidCourseDialog()
        return@chooseFile
      }
      saveLastImportLocation(file)

      val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfo(course)
      if (courseMetaInfo != null) {
        invokeLater {
          val result = Messages.showDialog(null,
                                           EduCoreBundle.message("action.import.local.course.dialog.text"),
                                           EduCoreBundle.message("action.import.local.course.dialog"),
                                           arrayOf(EduCoreBundle.message("action.import.local.course.dialog.cancel.text"),
                                                   EduCoreBundle.message("action.import.local.course.dialog.ok.text")),
                                           Messages.OK,
                                           Messages.getErrorIcon())
          if (result == Messages.NO) {
            CoursesStorage.getInstance().removeCourseByLocation(courseMetaInfo.location)
            course.name = createUnusedName(course.name)
            doImportNewCourse(course, component)
          }
          else if (result == Messages.OK) {
            closeDialog(component)
            val project = ProjectUtil.openProject(courseMetaInfo.location, null, true)
            ProjectUtil.focusProjectWindow(project, true)
          }

        }
        return@chooseFile
      }
      doImportNewCourse(course, component)
    }
  }

  private fun createUnusedName(initialName: String): String {
    val existingNames = CoursesStorage.getInstance().state.courses.map { it.name }.filter { it.startsWith(initialName) }
    var copyNumber = 1
    var newName = initialName
    while (existingNames.contains(newName)) {
      copyNumber++
      newName = "$initialName ($copyNumber)"
    }

    return newName
  }

  private fun doImportNewCourse(course: Course, component: Component?) {
    EduCounterUsageCollector.importCourseArchive()
    closeDialog(component)
    ImportCourseDialog(course).show()
  }

  private class ImportCourseDialog(private val course: Course) : JoinCourseDialog(course) {
    override fun isToShowError(errorState: ErrorState): Boolean = errorState !is ErrorState.NotLoggedIn

    override fun show() {
      super.show()
      val project = course.project ?: return
      // IS_LOCAL_COURSE = true info is stored in PropertiesComponent to keep it after course restart on purpose
      // not to show login widget for local course
      PropertiesComponent.getInstance(project).setValue(CCCreateCoursePreviewDialog.IS_LOCAL_COURSE, true)
    }
  }

  private fun closeDialog(component: Component?) {
    val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, component)
    dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
  }

  companion object {
    @NonNls
    private const val LAST_IMPORT_LOCATION = "Edu.LastImportLocation"

    @NonNls
    const val ACTION_ID = "Educational.ImportLocalCourse"

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
      Messages.showErrorDialog(
        EduCoreBundle.message("dialog.message.no.course.in.archive"),
        EduCoreBundle.message("dialog.title.failed.to.add.local.course")
      )
    }
  }
}
