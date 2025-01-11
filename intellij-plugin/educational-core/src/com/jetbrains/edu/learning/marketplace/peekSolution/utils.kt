package com.jetbrains.edu.learning.marketplace.peekSolution

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.canShowCommunitySolutions
import com.jetbrains.edu.learning.courseFormat.ext.hasCommunitySolutions
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

internal const val GOT_STUCK_WRONG_SUBMISSIONS_AMOUNT: Int = 3

internal fun updateVisibilityAndLoadCommunitySubmissionsIfNeeded(project: Project, task: Task, component: JComponent) {
  if (!task.canShowCommunitySolutions()) {
    project.invokeLater {
      component.isVisible = false
    }
    return
  }
  if (!task.hasCommunitySolutions()) {
    CompletableFuture.runAsync({
      SubmissionsManager.getInstance(project).loadCommunitySubmissions(task)
    }, ProcessIOExecutorService.INSTANCE)
  }
  project.invokeLater {
    component.isVisible = true
  }
}
