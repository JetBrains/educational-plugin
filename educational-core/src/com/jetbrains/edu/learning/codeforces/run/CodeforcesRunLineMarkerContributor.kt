package com.jetbrains.edu.learning.codeforces.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.codeforces.CodeforcesUtils

class CodeforcesRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    val project = element.project
    val psiFile = element.containingFile
    if (psiFile.firstChild != element) {
      return null
    }
    val virtualFile = psiFile.virtualFile
    val inputFile = CodeforcesUtils.getInputFile(project, virtualFile)

    if (inputFile != virtualFile) {
      return null
    }

    val actions = ExecutorAction.getActions()
    if (actions.isEmpty()) {
      return null
    }
    return Info(AllIcons.RunConfigurations.TestState.Run, actions) { el -> actions.map { getText(it, el) }.joinToString(separator = "\n") }
  }
}