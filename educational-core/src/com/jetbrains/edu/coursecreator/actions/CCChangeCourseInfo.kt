package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.messages.EduCoreBundle.lazyMessage
import com.jetbrains.edu.learning.yaml.configFileName

class CCChangeCourseInfo : DumbAwareAction(
  lazyMessage("action.edit.course.information.text"),
  lazyMessage("action.edit.course.information.description"),
  null
) {
  override fun update(event: AnActionEvent) {
    val project = event.project ?: return
    val presentation = event.presentation
    presentation.isEnabledAndVisible = false
    if (!isCourseCreator(project)) {
      return
    }
    val view = event.getData(LangDataKeys.IDE_VIEW) ?: return
    if (view.directories.isEmpty()) {
      return
    }
    presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val configFileName = course.configFileName
    val configFile = project.courseDir.findChild(configFileName)
    if (configFile == null) {
      Logger.getInstance(CCChangeCourseInfo::class.java).error("Failed to find course config file")
      return
    }
    FileEditorManager.getInstance(project).openFile(configFile, true)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
