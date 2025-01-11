package com.jetbrains.edu.learning.marketplace.peekSolution

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.canShowCommunitySolutions
import com.jetbrains.edu.learning.courseFormat.ext.hasCommunitySolutions
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.submissions.SubmissionsListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

internal const val GOT_STUCK_WRONG_SUBMISSIONS_AMOUNT: Int = 3

internal fun listenCommunitySubmissions(project: Project, task: Task, component: JComponent) {
  project.messageBus.connect().subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      if (!task.canShowCommunitySolutions()) {
        project.invokeLater {
          component.isVisible = false
        }
        return@runAsync
      }
      if (!task.hasCommunitySolutions()) {
        submissionsManager.loadCommunitySubmissions(task)
      }
      project.invokeLater {
        component.isVisible = true
      }
    }, ProcessIOExecutorService.INSTANCE)
  })
}
