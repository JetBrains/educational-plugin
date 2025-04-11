package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.*
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveAction
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt.isZip
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.addVendor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.io.File
import javax.swing.JComponent
import javax.swing.SwingConstants

class CCCreateCourseArchiveDialog(project: Project, courseName: String) : DialogWrapper(project) {
  private val panel: DialogPanel
  var locationPath: String
  var authorName: String
  var checkAllTasksFlag: Boolean = true

  init {
    title = EduCoreBundle.message("course.creator.create.archive.dialog.title")
    locationPath = getArchiveLocation(project, courseName)
    authorName = getAuthorInitialValue(project)

    panel = createPanel(project)
    init()
  }

  private fun createPanel(project: Project): DialogPanel {
    return panel {
      row {
        val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
          .withTitle(EduCoreBundle.message("course.creator.create.archive.location.title"))
        textFieldWithBrowseButton(fileChooserDescriptor, project)
          .label(EduCoreBundle.message("course.creator.create.archive.panel.location"))
          .bindText(::locationPath)
          .align(AlignX.FILL)
          .validationInfo { validateArchivePath(it.text) }
      }.layout(RowLayout.LABEL_ALIGNED)
      row {
        textField()
          .label(EduCoreBundle.message("course.creator.create.archive.panel.author"))
          .bindText(::authorName)
          .align(AlignX.FILL)
      }.layout(RowLayout.LABEL_ALIGNED)
      row {
        checkBox(EduCoreBundle.message("course.creator.create.archive.panel.check.all.tasks"))
          .bindSelected(::checkAllTasksFlag)
          .applyToComponent {
            horizontalTextPosition = SwingConstants.LEFT
          }
      }
    }
  }

  private fun ValidationInfoBuilder.validateArchivePath(text: String): ValidationInfo? {
    val file = File(text)
    if (file.exists()) {
      return warning(EduCoreBundle.message("course.creator.create.archive.invalid.location.file.exists"))
    }
    if (!isZip(text)) {
      return error(EduCoreBundle.message("course.creator.create.archive.invalid.location.wrong.extension"))
    }
    return null
  }

  override fun doOKAction() {
    panel.apply()
    super.doOKAction()
  }

  override fun postponeValidation(): Boolean {
    return false
  }

  override fun createCenterPanel(): JComponent {
    return panel
  }

  companion object {
    private fun getAuthorInitialValue(project: Project): String {
      val course = project.course
      if (course != null) {
        if (course.vendor == null) {
          course.addVendor()
        }
        val vendor = course.vendor
        if (vendor != null) {
          return vendor.name
        }
      }
      val savedAuthorName = PropertiesComponent.getInstance(project).getValue(CCCreateCourseArchiveAction.AUTHOR_NAME)
      if (savedAuthorName != null) {
        return savedAuthorName
      }
      val userName = System.getProperty("user.name")
      return if (userName != null) {
        StringUtil.capitalize(userName)
      }
      else EduCoreBundle.message("action.create.course.archive.author.field.initial.value")
    }

    private fun getArchiveLocation(project: Project, name: String): String {
      val location = PropertiesComponent.getInstance(project).getValue(CCCreateCourseArchiveAction.LAST_ARCHIVE_LOCATION)
      if (location != null) return location
      var sanitizedName = FileUtil.sanitizeFileName(name)
      if (sanitizedName.startsWith("_")) sanitizedName = EduNames.COURSE
      return project.basePath + "/" + sanitizedName + ".zip"
    }
  }
}
