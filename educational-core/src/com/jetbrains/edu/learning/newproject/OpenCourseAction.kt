package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OptionAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

abstract class OpenCourseActionBase(
  name: String,
  private val dialog: BrowseCoursesDialog,
  private val courseMode: String
) : AbstractAction(name) {

  override fun actionPerformed(e: ActionEvent) {
    val course = dialog.selectedCourse
    val projectSettings = dialog.projectSettings
    val location = dialog.locationString
    dialog.close(DialogWrapper.OK_EXIT_CODE)
    val configurator = EduConfiguratorManager.forLanguage(course.languageById)
    if (configurator != null) {
      course.courseMode = courseMode
      val projectGenerator = configurator
        .courseBuilder
        .getCourseProjectGenerator(course)
      projectGenerator?.doCreateCourseProject(location, projectSettings)
    }
  }
}

class ViewAsEducatorAction(dialog: BrowseCoursesDialog) : OpenCourseActionBase("View as Educator", dialog, CCUtils.COURSE_MODE)

class OpenCourseAction(dialog: BrowseCoursesDialog) : OpenCourseActionBase("Join", dialog, EduNames.STUDY), OptionAction {

  private val viewAsEducatorAction: ViewAsEducatorAction? = if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
    ViewAsEducatorAction(dialog)
  } else {
    null
  }

  override fun getOptions(): Array<Action> = if (viewAsEducatorAction == null) arrayOf() else arrayOf(viewAsEducatorAction)
}
