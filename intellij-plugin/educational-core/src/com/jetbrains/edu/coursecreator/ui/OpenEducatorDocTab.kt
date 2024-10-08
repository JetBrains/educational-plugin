package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider.Request.Companion.url
import com.intellij.openapi.project.Project
import com.intellij.util.Urls.newFromEncoded
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCOpenEducatorDocTab : AnAction() {
  private val documentationUrl = "https://github.com/jetbrains-academy/educators-guide-test/wiki"

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    // docs
    val title = EduCoreBundle.message("course.creator.docs.file")

    val request = url(newFromEncoded(documentationUrl).toExternalForm())
    openEditor(project, title, request)
  }

  private fun openEditor(project: Project, title: String, request: HTMLEditorProvider.Request) {
    HTMLEditorProvider.openEditor(project, title, request)
  }
}