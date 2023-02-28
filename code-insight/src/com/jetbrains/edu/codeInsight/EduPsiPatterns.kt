package com.jetbrains.edu.codeInsight

import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.learning.course

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> = PlatformPatterns.psiElement(I::class.java)

fun <T : Any, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.with(name: String, condition: (T, ProcessingContext?) -> Boolean): Self {
  return with(object : PatternCondition<T>(name) {
    override fun accepts(t: T, context: ProcessingContext?): Boolean = condition(t, context)
  })
}

fun <T : PsiElement, Self : PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.inCourse(): Self {
  return with("inCourse") { e, _ -> e.project.course != null }
}

fun <T : PsiElement, Self : PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.inFileWithName(vararg fileNames: String): Self {
  val namePattern = VirtualFilePattern().with("withNames") { file, _ -> file.name in fileNames }
  return inVirtualFile(namePattern)
}
