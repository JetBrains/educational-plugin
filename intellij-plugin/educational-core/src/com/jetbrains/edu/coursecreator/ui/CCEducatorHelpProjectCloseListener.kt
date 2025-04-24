package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCEducatorHelpProjectCloseListener : ProjectManagerListener {
  override fun projectClosing(project: Project) {
    val openFiles = FileEditorManager.getInstance(project).openFiles
    val educatorHelpFile = openFiles.find { it.url.endsWith(EduCoreBundle.message("course.creator.docs.file")) }

    if (educatorHelpFile != null) {
      PropertiesComponent.getInstance(project).setValue(EDUCATOR_HELP_WAS_OPENED, true)
    }
  }

  companion object {
    const val EDUCATOR_HELP_WAS_OPENED = "edu.course.creator.educator.help.was.opened"
  }
}