package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.testAction

class ReportCommunitySolutionActionTest : EduActionTestCase() {

  override fun setUp() {
    super.setUp()
    project.course?.isMarketplace = true

    Disposer.register(testRootDisposable) {
      project.course?.isMarketplace = false
    }
  }

  fun `test invisible and disabled with nullable DataContext`() {
    testAction(ReportCommunitySolutionAction.ACTION_ID, shouldBeEnabled = false)
  }

  fun `test invisible and disabled with regular diff`() {
    val simpleDiffRequestChain = simpleDiffRequestChain(project)
    val diffVirtualFile = ChainDiffVirtualFile(simpleDiffRequestChain, "Regular Chain Diff Virtual File")

    testAction(ReportCommunitySolutionAction.ACTION_ID, dataContext(diffVirtualFile), shouldBeEnabled = false)
  }

  fun `test visible and enabled`() {
    val simpleDiffRequestChain = simpleDiffRequestChain(project)
    simpleDiffRequestChain.putCommunityData(1, 100)
    val diffVirtualFile = ChainDiffVirtualFile(simpleDiffRequestChain, "Chain Diff Virtual File with User Data")

    testAction(ReportCommunitySolutionAction.ACTION_ID, dataContext(diffVirtualFile), shouldBeEnabled = true, runAction = false)
  }

  companion object {

    @Suppress("HardCodedStringLiteral")
    fun simpleDiffRequestChain(project: Project?): SimpleDiffRequestChain {
      val diffContentFactory = DiffContentFactory.getInstance()
      val currentContent = diffContentFactory.create(project, "Current Content")
      val anotherContent = diffContentFactory.create(project, "Another Content")

      return SimpleDiffRequestChain(listOf(SimpleDiffRequest("Simple Diff Request", currentContent, anotherContent, "1", "2")))
    }

    fun SimpleDiffRequestChain.putCommunityData(taskId: Int, solutionId: Int) {
      putUserData(ReportCommunitySolutionAction.TASK_ID_KEY, taskId)
      putUserData(ReportCommunitySolutionAction.SUBMISSION_ID_KEY, solutionId)
    }
  }
}