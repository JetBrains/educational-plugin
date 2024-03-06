package com.jetbrains.edu.kotlin.learning.eduAssistant.psi.context

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.eduAssistant.context.differ.FilesDiffer
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFilesDiffer : FilesDiffer {

  override fun findChangedMethods(before: PsiFile, after: PsiFile, taskFile: TaskFile): String {
    val beforeMethods = PsiTreeUtil.findChildrenOfType(before, KtNamedFunction::class.java).associateBy { it.generateSignature(SignatureSource.VISIBLE_FILE) }
    val afterMethods = PsiTreeUtil.findChildrenOfType(after, KtNamedFunction::class.java).associateBy { it.generateSignature(SignatureSource.VISIBLE_FILE) }

    val changedMethods = afterMethods.values.asSequence().filterNot { method ->
      val matchingMethod = beforeMethods[method.generateSignature(SignatureSource.VISIBLE_FILE)]
      matchingMethod != null && method.text == matchingMethod.text
    }.toList()

    taskFile.changedFunctionSignatures = changedMethods.mapNotNull { it.generateSignature(SignatureSource.VISIBLE_FILE) }.toSet()

    return changedMethods.joinToString(separator = System.lineSeparator()) { it.text }
  }
}
