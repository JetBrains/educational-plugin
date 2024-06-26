package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.psi.PsiFile

  /**
   * Interface representing a wrapper for PSI methods,
   * providing common API to handle method-related data for different languages.
   *
   * @property name The name of a method
   * @property methodDescriptor Human-readable method signature
   * @property text The text of the function
   * @property containingClass Class where the method is located
   * @property containingFile File where the method is located
   * */
  interface PsiMethodWrapper {
    val name: String
    val methodDescriptor: String
    val signature: String
    val text: String?
    val containingClass: PsiClassWrapper?
    val containingFile: PsiFile?

    /**
     * Checks if the given line number is within the range of the specified PsiMethod.
     *
     * @param lineNumber The line number to check.
     * @return `true` if the line number is within the range of the method, `false` otherwise.
     */
    fun containsLine(lineNumber: Int): Boolean
  }
