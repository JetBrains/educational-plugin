package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.actions.YamlActionsHelper
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import org.jetbrains.annotations.NonNls

open class CCEditAnswerPlaceholder : CCAnswerPlaceholderAction() {

  override fun performAnswerPlaceholderAction(state: EduState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    val task = answerPlaceholder.taskFile.task
    val configFileName = task.configFileName
    val taskDir = task.getDir(state.project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return
    }
    val configFile = taskDir.findChild(configFileName)
    if (configFile == null) {
      LOG.error("Failed to find task config file")
      return
    }
    FileEditorManager.getInstance(state.project).openFile(configFile, true)

    EP_NAME.computeSafeIfAny { yamlActionsHelper ->
      val yamlPsiFile = PsiManager.getInstance(state.project).findFile(configFile) ?: return@computeSafeIfAny
      val placeholderElement = yamlActionsHelper.findPsiElementCorrespondingToPlaceholder(yamlPsiFile, answerPlaceholder) ?: return@computeSafeIfAny
      placeholderElement.navigate(true)
    }
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    presentation.isEnabledAndVisible = eduState.answerPlaceholder != null
  }

  companion object {
    private val LOG = Logger.getInstance(CCEditAnswerPlaceholder::class.java)

    private val EP_NAME: ExtensionPointName<YamlActionsHelper> = ExtensionPointName.create("Educational.yamlActionsHelper")

    @NonNls
    const val ACTION_ID = "Educational.Educator.EditAnswerPlaceholder"
  }
}