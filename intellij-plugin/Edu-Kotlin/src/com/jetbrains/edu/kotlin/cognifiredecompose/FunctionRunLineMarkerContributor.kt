package com.jetbrains.edu.kotlin.cognifiredecompose

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.ui.DecomposeIcons
import com.jetbrains.edu.cognifiredecompose.actions.FunctionGeneratorAction
import com.jetbrains.edu.cognifiredecompose.utils.isIntroComment
import com.jetbrains.edu.kotlin.messages.EduKotlinBundle

class FunctionRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    if (element.isIntroComment()) {
      val action = FunctionGeneratorAction(element)
      val tooltipText = EduKotlinBundle.message("action.run.generate.function.text")
      return Info(
        DecomposeIcons.Function,
        arrayOf(action),
      ) { _ -> tooltipText }
    }
    return null
  }

}
