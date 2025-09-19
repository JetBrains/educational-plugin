package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.StartupUiUtil
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.actions.ActionWithButtonCustomComponent
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCOpenEducatorHelp : ActionWithButtonCustomComponent() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    doOpen(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.EDUCATOR_HELP) && JBCefApp.isSupported()
  }

  companion object {
    private const val HELP_URL = "/educatorsHelpPage/educatorsHelpPage.html"
    private const val THEME_KEY = $$"$__VISION_PAGE_SETTINGS_THEME__$"
    private const val DARK_THEME = "dark"
    private const val LIGHT_THEME = "light"

    fun doOpen(project: Project) {
      require(JBCefApp.isSupported()) { "JCEF is not supported on this system" }

      val welcomeScreenHtml = loadResourceAsString()?.replace(THEME_KEY, if (StartupUiUtil.isDarkTheme) DARK_THEME else LIGHT_THEME)
      welcomeScreenHtml ?: error("Couldn't load help file $HELP_URL")

      if (isFeatureEnabled(EduExperimentalFeatures.EDUCATOR_HELP)) {
        val openFiles = FileEditorManager.getInstance(project).openFiles
        val educatorHelpFile = openFiles.find { it.url.endsWith(EduCoreBundle.message("course.creator.docs.file")) }
        if (educatorHelpFile != null) {
          FileEditorManager.getInstance(project).openFile(educatorHelpFile)
        }
        else {
          val request = HTMLEditorProvider.Request.html(welcomeScreenHtml)
          openEditor(project, EduCoreBundle.message("course.creator.docs.file"), request)
        }
      }
    }

    private fun loadResourceAsString(): String? {
      val stream = this::class.java.getResourceAsStream(HELP_URL)
      val content = stream?.bufferedReader().use { it?.readText() }
      return content
    }

    private fun openEditor(project: Project, title: String, request: HTMLEditorProvider.Request) {
      HTMLEditorProvider.openEditor(project, title, request)
    }
  }
}