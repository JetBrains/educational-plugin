package com.jetbrains.edu.ai.refactoring.advisor.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.refactoring.advisor.EduAIRefactoringAdvisorService
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AIRefactoringAdvisorListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (result.isSolved) {
      EduAIRefactoringAdvisorService.getInstance(project).showRefactoringLinkInClippy()
    }
  }
}