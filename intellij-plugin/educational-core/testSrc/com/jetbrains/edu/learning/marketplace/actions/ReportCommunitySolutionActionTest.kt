package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TestDialog
import com.intellij.util.application
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import io.mockk.every
import org.junit.Test
import kotlin.random.Random

class ReportCommunitySolutionActionTest : EduActionTestCase() {
  override fun setUp() {
    super.setUp()

    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        eduTask(name = "task1", stepId = 1) {
          kotlinTaskFile("src/Task1.kt", "fun task1() {}")
          kotlinTaskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask(name = "task2", stepId = 2) {
          kotlinTaskFile("src/Task2.kt", "fun task2() {}")
          kotlinTaskFile("test/Tests2.kt", "fun tests2() {}")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse

    mockService<MarketplaceSubmissionsConnector>(application)
  }

  @Test
  fun `test invisible and disabled with nullable DataContext`() {
    testAction(ReportCommunitySolutionAction.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test invisible and disabled with regular diff`() {
    val simpleDiffRequestChain = simpleDiffRequestChain(project)
    val diffVirtualFile = ChainDiffVirtualFile(simpleDiffRequestChain, "Regular Chain Diff Virtual File")

    testAction(ReportCommunitySolutionAction.ACTION_ID, dataContext(diffVirtualFile), shouldBeEnabled = false)
  }

  @Test
  fun `test visible and enabled`() {
    val simpleDiffRequestChain = simpleDiffRequestChain(project)
    simpleDiffRequestChain.putCommunityData(1, 100)
    val diffVirtualFile = ChainDiffVirtualFile(simpleDiffRequestChain, "Chain Diff Virtual File with User Data")

    testAction(ReportCommunitySolutionAction.ACTION_ID, dataContext(diffVirtualFile), shouldBeEnabled = true, runAction = false)
  }

  @Test
  fun `test report community solution action success`() {
    // setup
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    every { mockedService.reportSolution(any()) } returns true

    // add solution from the community
    val taskId = Random.nextInt()
    val communitySolutions = communitySolutions(taskId)
    val submissionsManager = SubmissionsManager.getInstance(project)
    val communityIds = communitySolutions.map { it.id as Int }
    val solutionToReport = communitySolutions.random()
    val solutionToReportId = solutionToReport.id as Int
    submissionsManager.addCommunitySolutions(taskId, communitySolutions)
    checkCommunitySolutionsPresented(taskId, communityIds)

    // when
    withTestDialog(TestDialog.YES) {
      testAction(ReportCommunitySolutionAction.ACTION_ID, reportActionDataContext(project, solutionToReport, solutionToReportId))
    }

    // then
    checkCommunitySolutionsPresented(taskId, communityIds.filter { it != solutionToReportId })
    checkCommunitySolutionNotPresented(taskId, solutionToReportId)
  }

  @Test
  fun `test report community solution action failed`() {
    // setup
    val mockedService = MarketplaceSubmissionsConnector.getInstance()
    every { mockedService.reportSolution(any()) } returns false

    // add solution from the community
    val taskId = Random.nextInt()
    val communitySolutions = communitySolutions(taskId)
    val submissionsManager = SubmissionsManager.getInstance(project)
    val communityIds = communitySolutions.map { it.id as Int }
    val solutionToReport = communitySolutions.random()
    val solutionToReportId = solutionToReport.id as Int
    submissionsManager.addCommunitySolutions(taskId, communitySolutions)
    checkCommunitySolutionsPresented(taskId, communityIds)

    // when
    withTestDialog(TestDialog.YES) {
      testAction(ReportCommunitySolutionAction.ACTION_ID, reportActionDataContext(project, solutionToReport, solutionToReportId))
    }

    // then
    checkCommunitySolutionsPresented(taskId, communityIds)
  }

  private fun checkCommunitySolutionsPresented(taskId: Int, solutionsIds: List<Int>) {
    val solutionsIdsFromMemory =
      SubmissionsManager.getInstance(project).getCommunitySubmissionsFromMemory(taskId)?.mapNotNull { it.id }?.toSet()
    checkNotNull(solutionsIdsFromMemory)
    assertTrue(solutionsIds.all { solutionsIdsFromMemory.contains(it) })
  }

  private fun checkCommunitySolutionNotPresented(taskId: Int, solutionId: Int) {
    val solutionsIdsFromMemory =
      SubmissionsManager.getInstance(project).getCommunitySubmissionsFromMemory(taskId)?.mapNotNull { it.id }?.toSet()
    checkNotNull(solutionsIdsFromMemory)
    assertTrue(solutionId !in solutionsIdsFromMemory)
  }

  private fun reportActionDataContext(project: Project, solution: Submission, solutionId: Int): DataContext {
    val diffChain = simpleDiffRequestChain(project)
    diffChain.putCommunityData(solution.taskId, solutionId)

    val diffVirtualFile = ChainDiffVirtualFile(diffChain, "")
    return SimpleDataContext.builder()
      .add(CommonDataKeys.VIRTUAL_FILE, diffVirtualFile)
      .add(CommonDataKeys.PROJECT, project)
      .build()
  }

  private fun communitySolutions(taskId: Int): MutableList<Submission> = mutableListOf(
    MarketplaceSubmission(taskId, CheckStatus.Solved, "some solution", null, 1).apply {
      id = Random.nextInt()
    },
    MarketplaceSubmission(taskId, CheckStatus.Solved, "some solution 2", null, 1).apply {
      id = Random.nextInt()
    },
    MarketplaceSubmission(taskId, CheckStatus.Solved, "some solution 3", null, 1).apply {
      id = Random.nextInt()
    }
  )

  private fun SimpleDiffRequestChain.putCommunityData(taskId: Int, solutionId: Int) {
    putUserData(ReportCommunitySolutionAction.TASK_ID_KEY, taskId)
    putUserData(ReportCommunitySolutionAction.SUBMISSION_ID_KEY, solutionId)
  }
}