package com.jetbrains.edu.smartSearch.ui

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.InlineBanner
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.marketplace.loadMarketplaceCourseStructure
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider.Companion.joinCourse
import com.jetbrains.edu.learning.newproject.ui.coursePanel.openCourse
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel.Companion.nameToLocation
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.edu.smartSearch.connector.SmartSearchService
import com.jetbrains.edu.smartSearch.messages.EduSmartSearchBundle
import org.jdesktop.swingx.VerticalLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

class SmartSearchResultDialog(
  private val result: List<SmartSearchService.CourseTaskData>
) : DialogWrapper(true) {
  init {
    title = EduSmartSearchBundle.message("dialog.smart.search.result.title")
    isAutoAdjustable = true
    init()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel(VerticalLayout(10))
    for (taskData in result) {
      val inlineBanner = PythonTaskInlineBanner().apply {
        setMessage("Task \"${taskData.taskName}\" in <b>${taskData.courseName}<b> course")
        addAction("Open") {
          openTaskInCourse(taskData)
        }
      }
      panel.add(inlineBanner)
    }
    return panel
  }

  private fun openTaskInCourse(courseTaskData: SmartSearchService.CourseTaskData) {
    invokeLater {
      val course = EduCourse().apply {
        name = courseTaskData.courseName
        id = courseTaskData.marketplaceId
        courseMode = CourseMode.STUDENT
        languageId = EduFormatNames.PYTHON
        isMarketplace = true
      }
      // Try to open locally or download from Marketplace
      val project = course.openCourse() ?: run {
        logger<SmartSearchDialog.SmartSearchDialogResult>().info("Failed to open project for course ${courseTaskData.courseName}")
        val courseInfo = CourseCreationInfo(course, nameToLocation(course), course.configurator?.courseBuilder?.getLanguageSettings()?.getSettings())
        courseInfo.course.loadMarketplaceCourseStructure()
        joinCourse(courseInfo, CourseMode.STUDENT, null) {}
        course.openCourse() ?: error("Failed to download and open course")
      }
      val task = project.course?.allTasks?.find { it.name == courseTaskData.taskName } ?: error("Task is null")
      NavigationUtils.navigateToTask(project, task)
    }
  }

  class PythonTaskInlineBanner : InlineBanner() {
    init {
      setIcon(EducationalCoreIcons.Language.Python)
      isOpaque = false
      border = BorderFactory.createCompoundBorder(
        RoundedLineBorder(
          EduColors.aiGetHintInlineBannersBorderColor, NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()
        ), JBUI.Borders.empty(10)
      )
      background = EduColors.aiGetHintInlineBannersBackgroundColor
    }
  }
}