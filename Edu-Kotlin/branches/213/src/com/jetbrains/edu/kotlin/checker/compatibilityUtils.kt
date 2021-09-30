package com.jetbrains.edu.kotlin.checker

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.junit.JunitKotlinTestFrameworkProvider

// BACKCOMPAT: 2021.2. Inline it
fun getTestClass(element: PsiElement): PsiClass? {
  return JunitKotlinTestFrameworkProvider.getJavaTestEntity(element, checkMethod = false)?.testClass
}
