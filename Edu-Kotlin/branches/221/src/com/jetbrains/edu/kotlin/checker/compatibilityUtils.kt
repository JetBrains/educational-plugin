package com.jetbrains.edu.kotlin.checker

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.extensions.KotlinTestFrameworkProvider

// BACKCOMPAT: 2021.2. Inline it
fun getTestClass(element: PsiElement): PsiClass? {
  for (extension in KotlinTestFrameworkProvider.EP_NAME.extensionList) {
    return extension.getJavaTestEntity(element, checkMethod = false)?.testClass ?: continue
  }
  return null
}
