package com.jetbrains.edu.kotlin.cognifire

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.cognifire.actions.PromptExecutorAction
import com.jetbrains.edu.cognifire.manager.PromptActionManager
import com.jetbrains.edu.cognifire.manager.PromptCodeState
import com.jetbrains.edu.cognifire.ui.CognifireIcons
import com.jetbrains.edu.cognifire.utils.PROMPT
import com.jetbrains.edu.cognifire.utils.isPromptBlock
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class PromptRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    val project = element.project
    val task = project.getCurrentTask()
    val targetElement = element.parent.parent
    if (element is LeafPsiElement && targetElement is KtCallExpression && element.text == PROMPT && targetElement.isPromptBlock()) {
      val function = PsiTreeUtil.getParentOfType(targetElement, KtNamedFunction::class.java) ?: return null
      val uniqueId = "${function.fqName}:${function.valueParameters.size}" // one prompt per function
      val action = PromptExecutorAction(targetElement, uniqueId, task)
      var tooltipText = EduKotlinBundle.message("action.run.generate.code.text")
      val promptActionManager = PromptActionManager.getInstance(project)
      val promptAction = promptActionManager.getAction(uniqueId)
      if (promptAction == null) {
        promptActionManager.addAction(uniqueId, task?.id)
      } else if (promptAction.state != PromptCodeState.PromptWritten) {
        tooltipText = EduKotlinBundle.message("action.run.sync.prompt.with.code.text")
      }
      task?.let {
        it.isPromptActionsGeneratedSuccessfully = promptActionManager.generatedSuccessfully(it.id)
        TaskToolWindowView.getInstance(project).updateCheckPanel(it)
      }
      return Info(
        CognifireIcons.Sync,
        arrayOf(action),
      ) { _ -> tooltipText }
    }
    return null
  }
}
