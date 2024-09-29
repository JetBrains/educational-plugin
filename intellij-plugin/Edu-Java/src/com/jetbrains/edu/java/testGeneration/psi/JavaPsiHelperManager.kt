package com.jetbrains.edu.java.testGeneration.psi

import com.intellij.psi.PsiFile
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.psi.manager.PsiHelperManager

class JavaPsiHelperManager : PsiHelperManager {
  override fun getPsiHelper(psiFile: PsiFile): PsiHelper {
    return JavaPsiHelper(psiFile)
  }
}
