package com.jetbrains.edu.learning.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask


class ChoiceTaskChecker(task: ChoiceTask, project: Project) : TaskChecker<ChoiceTask>(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    val correctOptionIndices = task.choiceOptions.mapIndexedNotNull { index, value -> if (value.status == ChoiceTask.OptionStatus.CORRECT) index else null }
    val allCorrectOptionsSelected = correctOptionIndices.size == task.selectedVariants.size
                                    && correctOptionIndices.containsAll(task.selectedVariants)
    return if (allCorrectOptionsSelected) CheckResult.SOLVED else CheckResult(CheckStatus.Failed, "Incorrect")
  }
}