package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import icons.EducationalCoreIcons
import javax.swing.Icon
import javax.swing.JPanel

private const val BEFORE_LINK = "beforeLink"
private const val LINK = "link"
private const val LINK_TEXT = "linkText"
private const val AFTER_LINK = "afterLink"
private val LINK_ERROR_PATTERN: Regex = """(?<$BEFORE_LINK>.*)<a href="(?<$LINK>.*)">(?<$LINK_TEXT>.*)</a>(?<$AFTER_LINK>.*)""".toRegex()


class JetBrainsAcademyPlatformProvider : CoursesPlatformProvider() {
  override val name: String = EduNames.JBA

  override val icon: Icon get() = EducationalCoreIcons.JB_ACADEMY_TAB

  override val panel get() = JetBrainsAcademyCoursesPanel(this)

  override fun joinAction(courseInfo: CourseInfo,
                          courseMode: CourseMode,
                          coursePanel: JPanel) {

    val account = HyperskillSettings.INSTANCE.account ?: return

    HyperskillProjectAction.openHyperskillProject(account) { errorMessage ->
      val groups = LINK_ERROR_PATTERN.matchEntire(errorMessage)?.groups
      val errorState = if (groups == null) ErrorState.CustomSevereError(errorMessage)
      else ErrorState.CustomSevereError(groups.valueOrEmpty(BEFORE_LINK),
                                        groups.valueOrEmpty(LINK_TEXT),
                                        groups.valueOrEmpty(AFTER_LINK),
                                        Runnable { BrowserUtil.browse(groups.valueOrEmpty(LINK)) })
      panel.setError(errorState)
    }

    val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, coursePanel)
    dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
  }

  override suspend fun loadCourses(): List<Course> {
    return SUPPORTED_LANGUAGES.mapNotNull { languageId ->
      val provider = CourseCompatibilityProviderEP.find(languageId, EduNames.DEFAULT_ENVIRONMENT) ?: return@mapNotNull null
      JetBrainsAcademyCourse(languageId, provider.technologyName)
    }
  }

  private fun MatchGroupCollection.valueOrEmpty(groupName: String): String = this[groupName]?.value ?: ""

  companion object {
    private val SUPPORTED_LANGUAGES = listOf(
      EduNames.JAVA,
      EduNames.KOTLIN,
      EduNames.PYTHON
    )
  }
}