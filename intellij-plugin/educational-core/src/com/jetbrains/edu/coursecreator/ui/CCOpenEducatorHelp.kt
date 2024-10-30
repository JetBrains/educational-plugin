package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider.Request.Companion.url
import com.intellij.openapi.project.Project
import com.intellij.util.Urls.newFromEncoded
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCOpenEducatorHelp : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    doOpen(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.EDUCATOR_HELP)
  }

  companion object {
    private const val DOCUMENTATION_URL = "https://jetbrains-academy.github.io/educators-guide-test/"

    fun doOpen(project: Project) {
      if (isFeatureEnabled(EduExperimentalFeatures.EDUCATOR_HELP)) {
        val request = url(newFromEncoded(DOCUMENTATION_URL).toExternalForm())
        val openFiles = FileEditorManager.getInstance(project).openFiles
        val educatorHelpFile = openFiles.find { it.url.endsWith(EduCoreBundle.message("course.creator.docs.file")) }
        if (educatorHelpFile != null) {
          FileEditorManager.getInstance(project).openFile(educatorHelpFile)
        }
        else {
          openEditor(project, EduCoreBundle.message("course.creator.docs.file"), request)
        }
      }
    }

    private fun openEditor(project: Project, title: String, request: HTMLEditorProvider.Request) {
      HTMLEditorProvider.openEditor(project, title, request)
    }
  }
}