package com.jetbrains.edu.kotlin.jarvis.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker

fun findBlock(element: PsiElement,
                      nextElement: (PsiElement) -> PsiElement?,
                      blockStartText: String): PsiElement? {
  var possibleBlock = nextElement(element)
  while (
    possibleBlock != null &&
    !(possibleBlock.text.startsWith(blockStartText) &&
      JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(possibleBlock, possibleBlock.language))
  ) {
    possibleBlock = nextElement(possibleBlock)
  }
  return possibleBlock
}

const val DRAFT = "draft"
const val DESCRIPTION = "description"
