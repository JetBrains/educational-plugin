package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OptionAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.OpenCourseDialogBase
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

abstract class OpenCourseActionBase(
  name: String,
  private val dialog: OpenCourseDialogBase,
  private val courseMode: String
) : AbstractAction(name) {

  override fun actionPerformed(e: ActionEvent) {
    val (course, location, projectSettings) = dialog.courseInfo
    val configurator = course?.configurator
    if (configurator != null) {
      try {
        configurator.beforeCourseStarted(course)
        dialog.close(DialogWrapper.OK_EXIT_CODE)
        course.courseMode = courseMode
        val projectGenerator = configurator
          .courseBuilder
          .getCourseProjectGenerator(course)
        projectGenerator?.doCreateCourseProject(location, projectSettings)
      }
      catch (e: CourseCantBeStartedException) {
        dialog.setError(e.error)
      }
    }
  }
}

class ViewAsEducatorAction(dialog: OpenCourseDialogBase) : OpenCourseActionBase("View as Educator", dialog, CCUtils.COURSE_MODE)

class OpenCourseAction(name: String, dialog: OpenCourseDialogBase, allowViewAsEducatorAction: Boolean) : OpenCourseActionBase(name, dialog, EduNames.STUDY), OptionAction {

  val viewAsEducatorAction: ViewAsEducatorAction? = if (allowViewAsEducatorAction && CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
    ViewAsEducatorAction(dialog)
  } else {
    null
  }

  override fun getOptions(): Array<Action> = if (viewAsEducatorAction == null) arrayOf() else arrayOf(viewAsEducatorAction)

}
