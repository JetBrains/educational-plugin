package com.jetbrains.edu.ai.debugger.kotlin.slice

import com.intellij.psi.PsiElement

typealias MutablePsiElementToDependencies = MutableMap<PsiElement, HashSet<PsiElement>>
typealias PsiElementToDependencies = Map<PsiElement, HashSet<PsiElement>>

abstract class FunctionDependency {
  val dependenciesForward = mutableMapOf<PsiElement, HashSet<PsiElement>>()
  val dependenciesBackward = mutableMapOf<PsiElement, HashSet<PsiElement>>()

  abstract fun PsiElement.addDependency(other: PsiElement)
}
