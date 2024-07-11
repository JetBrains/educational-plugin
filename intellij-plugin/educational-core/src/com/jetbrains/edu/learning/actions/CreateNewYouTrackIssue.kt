package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.ext.technologyName
import com.jetbrains.edu.learning.pluginVersion

@Suppress("ComponentNotRegistered")
class CreateNewYouTrackIssue : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = ApplicationManager.getApplication().isInternal ||
                                         project != null && project.isEduProject()
  }

 override fun getActionUpdateThread() = ActionUpdateThread.BGT

 override fun actionPerformed(e: AnActionEvent) {
    val course = e.project?.course
    val description = createIssueDescription(course)
    val link = "https://youtrack.jetbrains.com/newIssue?project=EDU&description=${URLUtil.encodeURIComponent(description)}"
    EduBrowser.getInstance().browse(link)
  }

  companion object {

    private fun createIssueDescription(course: Course?): String {
      val pluginVersion = pluginVersion(EduNames.PLUGIN_ID)!!
      return buildString {
        appendLine("""
          ## Environment
          
          * **JetBrains Academy plugin version**: $pluginVersion
          * **IDE name and version**: $ideNameAndVersion
          * **Operating system**: ${SystemInfo.getOsNameAndVersion()}
        """.trimIndent())
        if (course != null) {
          appendLine("""
            * **Course name**: ${course.name}
            * **Course info**: ${course.courseInfo}
            * **Course mode**: ${course.mode}
            """.trimIndent())
        }
        appendLine("""
          
          ## Problem description
          
          ## Steps to reproduce
        """.trimIndent())
      }
    }

    private val ideNameAndVersion: String
      get() {
        val appInfo = ApplicationInfo.getInstance()
        val appName = appInfo.fullApplicationName
        val editionName = ApplicationNamesInfo.getInstance().editionName
        val ideVersion = appInfo.build.toString()
        return buildString {
          append(appName)
          if (editionName != null) {
            append(" ")
            append(editionName)
          }
          append(" (")
          append(ideVersion)
          append(")")
        }
      }

    private val Course.courseInfo: String
      get() {
        val environment = course.environment
        return buildString {
          technologyName?.let {
            append(it)
            append(" ")
          }
          append(itemType)
          if (environment != DEFAULT_ENVIRONMENT) {
            append(" (")
            append(environment)
            append(")")
          }
        }
      }

    private val Course.mode: String get() = if (isStudy) "Learner" else "Educator"
  }
}
