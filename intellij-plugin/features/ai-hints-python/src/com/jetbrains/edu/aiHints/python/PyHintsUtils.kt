package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.python.psi.PyFunction

object PyHintsUtils {
  @RequiresReadLock
  fun PsiFile.functions(): Collection<PyFunction> = PsiTreeUtil.findChildrenOfType(this, PyFunction::class.java)
}