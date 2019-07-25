package com.jetbrains.edu.yaml

import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> = PlatformPatterns.psiElement(I::class.java)

fun <T, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.with(name: String, condition: (T, ProcessingContext?) -> Boolean): Self {
  return with(object : PatternCondition<T>(name) {
    override fun accepts(t: T, context: ProcessingContext?): Boolean = condition(t, context)
  })
}

fun <T : PsiElement, Self : PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.inFileWithName(fileName: String): Self {
  return inVirtualFile(VirtualFilePattern().withName(fileName))
}

fun keyValueWithName(keyText: String): PsiElementPattern.Capture<YAMLKeyValue> {
  return psiElement<YAMLKeyValue>()
    .with("${keyText}_pattern") { element, _ -> element.keyText == keyText }
}
