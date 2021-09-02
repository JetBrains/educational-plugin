package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.configFileName
import org.jetbrains.annotations.NonNls

open class CCEditAnswerPlaceholder :
  CCAnswerPlaceholderAction(EduCoreBundle.lazyMessage("action.edit.answer.placeholder.text"),
                            EduCoreBundle.lazyMessage("action.edit.answer.placeholder.description")) {

  override fun performAnswerPlaceholderAction(project: Project, state: EduState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    val task = answerPlaceholder.taskFile.task
    val configFileName = task.configFileName
    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return
    }
    val configFile = taskDir.findChild(configFileName)
    if (configFile == null) {
      LOG.error("Failed to find task config file")
      return
    }
    FileEditorManager.getInstance(project).openFile(configFile, true)
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    presentation.isEnabledAndVisible = eduState.answerPlaceholder != null
  }

  companion object {
    private val LOG = Logger.getInstance(CCEditAnswerPlaceholder::class.java)

    @NonNls
    const val ACTION_ID = "Educational.Educator.EditAnswerPlaceholder"
  }
}