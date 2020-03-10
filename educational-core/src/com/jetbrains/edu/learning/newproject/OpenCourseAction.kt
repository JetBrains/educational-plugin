package com.jetbrains.edu.learning.newproject

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OptionAction
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.OpenCourseDialogBase
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_URL
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

private const val BEFORE_LINK = "beforeLink"
private const val LINK = "link"
private const val LINK_TEXT = "linkText"
private const val AFTER_LINK = "afterLink"
private val LINK_ERROR_PATTERN: Regex = """(?<$BEFORE_LINK>.*)<a href="(?<$LINK>.*)">(?<$LINK_TEXT>.*)</a>(?<$AFTER_LINK>.*)""".toRegex()
private fun MatchGroupCollection.valueOrEmpty(groupName: String): String = this[groupName]?.value ?: ""

abstract class OpenCourseActionBase(
  name: String,
  private val dialog: OpenCourseDialogBase,
  private val courseMode: String
) : AbstractAction(name) {

  override fun actionPerformed(e: ActionEvent) {
    joinCourse(dialog.courseInfo,
               courseMode,
               { dialog.setError(it) },
               { dialog.close(DialogWrapper.OK_EXIT_CODE) })
  }
}

class ViewAsEducatorAction(dialog: OpenCourseDialogBase) : OpenCourseActionBase("View as Educator", dialog, CCUtils.COURSE_MODE)

class OpenCourseAction(
  name: String,
  dialog: OpenCourseDialogBase,
  allowViewAsEducatorAction: Boolean
) : OpenCourseActionBase(name, dialog, EduNames.STUDY), OptionAction {

  val viewAsEducatorAction: ViewAsEducatorAction? = if (allowViewAsEducatorAction && CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
    ViewAsEducatorAction(dialog)
  }
  else {
    null
  }

  override fun getOptions(): Array<Action> = if (viewAsEducatorAction == null) arrayOf() else arrayOf(viewAsEducatorAction)

}

fun joinCourse(courseInfo: CourseInfo,
               courseMode: String,
               errorHandler: (ErrorState) -> Unit,
               closeDialogAction: () -> Unit = {}) {
  val (course, location, projectSettings) = courseInfo

  if (course is JetBrainsAcademyCourse) {
    joinJetBrainsAcademy(course, errorHandler)
    return
  }

  // location is null for course preview dialog only
  if (location == null) {
    return
  }
  val configurator = course.configurator
  if (configurator != null) {
    try {
      configurator.beforeCourseStarted(course)
      closeDialogAction()
      course.courseMode = courseMode
      val projectGenerator = configurator
        .courseBuilder
        .getCourseProjectGenerator(course)
      projectGenerator?.doCreateCourseProject(location, projectSettings)
    }
    catch (e: CourseCantBeStartedException) {
      errorHandler(e.error)
    }
  }
}

private fun joinJetBrainsAcademy(course: JetBrainsAcademyCourse, errorHandler: (ErrorState) -> Unit) {
  val account = HyperskillSettings.INSTANCE.account
  if (account == null) {
    BrowserUtil.browse(
      "${HYPERSKILL_DEFAULT_URL}onboarding/?track=${StringUtil.toLowerCase(course.language)}&utm_source=ide&utm_content=browse-courses")
  }
  else {
    HyperskillProjectAction.openHyperskillProject(account) { errorMessage ->
      val groups = LINK_ERROR_PATTERN.matchEntire(errorMessage)?.groups
      val errorState = if (groups == null) ErrorState.CustomSevereError(errorMessage)
      else ErrorState.CustomSevereError(groups.valueOrEmpty(BEFORE_LINK),
                                        groups.valueOrEmpty(LINK_TEXT),
                                        groups.valueOrEmpty(AFTER_LINK),
                                        Runnable { BrowserUtil.browse(groups.valueOrEmpty(LINK)) })
      errorHandler(errorState)
    }
  }
}
