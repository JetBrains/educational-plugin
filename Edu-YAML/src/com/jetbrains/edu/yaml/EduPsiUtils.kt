package com.jetbrains.edu.yaml

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

// BACKCOMPAT: 2019.1. Use platform method instead
inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = PsiTreeUtil.getParentOfType(this, T::class.java)
