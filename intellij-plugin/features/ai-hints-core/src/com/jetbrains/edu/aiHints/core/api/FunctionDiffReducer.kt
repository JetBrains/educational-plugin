package com.jetbrains.edu.aiHints.core.api

import com.intellij.psi.PsiElement

interface FunctionDiffReducer {
  /**
   * Creates a reduced version of changes between student's code and the suggested hint.
   * Instead of showing all changes at once, this function helps break down the hint into smaller,
   * more manageable suggestions.
   *
   * @param function The original function from student's code. Can be null if the hint suggests
   *                adding a completely new function that doesn't exist in student's code yet.
   * @param modifiedFunction The suggested version of the function from the generated hint.
   *                        This is a PSI element that represents the target state of the code.
   * @return A [PsiElement] representing a changes to be suggested to the student or null if no meaningful reduction can be made.
   */
  fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement?
}