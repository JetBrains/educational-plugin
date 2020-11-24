package com.jetbrains.edu.yaml

import com.intellij.patterns.PsiElementPattern
import com.jetbrains.edu.learning.codeInsight.psiElement
import com.jetbrains.edu.learning.codeInsight.with
import org.jetbrains.yaml.psi.YAMLKeyValue

fun keyValueWithName(keyText: String): PsiElementPattern.Capture<YAMLKeyValue> {
  return psiElement<YAMLKeyValue>()
    .with("${keyText}_pattern") { element, _ -> element.keyText == keyText }
}
