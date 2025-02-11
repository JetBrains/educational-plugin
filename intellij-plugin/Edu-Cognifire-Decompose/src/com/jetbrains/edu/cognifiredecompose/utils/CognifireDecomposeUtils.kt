package com.jetbrains.edu.cognifiredecompose.utils

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.CognifireDecomposeDslPackageCallChecker

fun PsiElement.isFunctionBlock() = runReadAction {
  text.startsWith(FUNCTION) && CognifireDecomposeDslPackageCallChecker.isCallFromCognifireDecomposeDslPackage(this, this.language)
}

fun PsiElement.isIntroComment() = runReadAction {
  this is PsiComment && this.text.contains(INTRO_COMMENT)
}

const val FUNCTION = "function"
const val INTRO_COMMENT = "Click the + icon to add a new function"