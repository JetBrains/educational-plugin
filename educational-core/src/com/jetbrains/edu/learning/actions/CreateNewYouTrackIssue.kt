package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.ext.technologyName
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered")  // registered in educational-core.xml
class CreateNewYouTrackIssue : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.Educational.CreateNewIssue.text"),
  EduCoreBundle.lazyMessage("action.Educational.CreateNewIssue.description"),
  null
) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!EduUtils.isEduProject(project)) return
    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val course = e.project?.course ?: return
    val pluginVersion = pluginVersion(EduNames.PLUGIN_ID)!!
    val ideNameAndVersion = ideNameAndVersion
    val os = SystemInfo.getOsNameAndVersion()
    val description = ISSUE_TEMPLATE.format(pluginVersion, ideNameAndVersion, os, course.name, course.courseInfo, course.mode)
    val link = "https://youtrack.jetbrains.com/newIssue?project=EDU&description=${URLUtil.encodeURIComponent(description)}"
    EduBrowser.getInstance().browse(link)
  }

  companion object {
    private val ISSUE_TEMPLATE = """
      ## Environment

      * **EduTools plugin version:** %s
      * **IDE name and version:** %s
      * **Operating system:** %s
      * **Course name**: %s
      * **Course info**: %s
      * **Course mode**: %s

      ## Problem description

      ## Steps to reproduce
    """.trimIndent()

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
