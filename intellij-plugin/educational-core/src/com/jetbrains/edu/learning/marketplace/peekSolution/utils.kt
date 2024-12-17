package com.jetbrains.edu.learning.marketplace.peekSolution

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.submissions.SubmissionsListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

internal fun listenCommunitySubmissions(project: Project, task: Task, component: JComponent) {
  project.messageBus.connect().subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      if (!submissionsManager.isAllowedToLoadCommunitySolutions(task)) {
        project.invokeLater {
          component.isVisible = false
        }
        return@runAsync
      }
      if (!submissionsManager.isCommunitySolutionsLoaded(task)) {
        submissionsManager.loadCommunitySubmissions(task)
      }
      project.invokeLater {
        component.isVisible = true
      }
    }, ProcessIOExecutorService.INSTANCE)
  })
}
