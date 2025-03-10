package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction

fun getFunctionsPsi(files: List<PsiFile>)
  =  files.map { PsiTreeUtil.findChildrenOfType(it, KtNamedFunction::class.java) }.flatten()