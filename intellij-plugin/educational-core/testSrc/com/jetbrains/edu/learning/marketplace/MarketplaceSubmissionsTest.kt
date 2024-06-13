package com.jetbrains.edu.learning.marketplace

import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.actions.ReportCommunitySolutionAction
import com.jetbrains.edu.learning.marketplace.actions.ReportCommunitySolutionActionTest.Companion.putCommunityData
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsList
import com.jetbrains.edu.learning.marketplace.api.SubmissionsService
import com.jetbrains.edu.learning.marketplace.deleteSubmissions.AdvancedSubmissionsDeleteDialog
import com.jetbrains.edu.learning.marketplace.deleteSubmissions.DeleteAllSubmissionsAction
import com.jetbrains.edu.learning.marketplace.deleteSubmissions.SubmissionsDeleteDialog
import com.jetbrains.edu.learning.marketplace.deleteSubmissions.deleteSubmissionsWithTestDialog
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.simpleDiffRequestChain
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.submissions.*
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withTestDialog
import io.mockk.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.intellij.lang.annotations.Language
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpURLConnection
import kotlin.random.Random

class MarketplaceSubmissionsTest : SubmissionsTestBase() {

  override fun setUp() {
    super.setUp()
    loginFakeMarketplaceUser()
    val settings = MarketplaceSettings.INSTANCE
    val initialAgreementState = settings.userAgreementState
    settings.setTestAgreementState(UserAgreementState.ACCEPTED)
    Disposer.register(testRootDisposable) {
      settings.setTestAgreementState(initialAgreementState)
    }
  }

  /**
   * Not that meaningful since [SubmissionsManager] stores only submissions for current course
   */
  @Test
  fun `test delete all submissions`() {
    configureSubmissionsResponses(submissionsLists = listOf(), submissionsDeleteRequestSuccess = true)

    val course = createEduCourse()
    val taskIds = course.allTasks.map { it.id }
    val submissionsManager = SubmissionsManager.getInstance(project)
    taskIds.forEach { taskId ->
      assertEmpty(submissionsManager.getSubmissionsFromMemory(setOf(taskId)))
      submissionsManager.addToSubmissions(taskId, generateMarketplaceSubmission())
      checkSubmissionsPresent(submissionsManager, taskId)
    }

    deleteSubmissionsWithTestDialog(object : SubmissionsDeleteDialog {
      override fun showWithResult(): Int = AdvancedSubmissionsDeleteDialog.ALL
    }) {
      testAction(DeleteAllSubmissionsAction.ACTION_ID)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    taskIds.forEach { taskId ->
      assertEmpty(submissionsManager.getSubmissionsFromMemory(setOf(taskId)))
    }
  }

  @Test
  fun `test delete course submissions`() {
    configureSubmissionsResponses(submissionsLists = listOf(), submissionsDeleteRequestSuccess = true)

    val course = createEduCourse()
    val taskIds = course.allTasks.map { it.id }
    val submissionsManager = SubmissionsManager.getInstance(project)
    taskIds.forEach { taskId ->
      assertEmpty(submissionsManager.getSubmissionsFromMemory(setOf(taskId)))
      submissionsManager.addToSubmissions(taskId, generateMarketplaceSubmission())
      checkSubmissionsPresent(submissionsManager, taskId)
    }

    deleteSubmissionsWithTestDialog(object : SubmissionsDeleteDialog {
      override fun showWithResult(): Int = AdvancedSubmissionsDeleteDialog.COURSE
    }) {
      testAction(DeleteAllSubmissionsAction.ACTION_ID)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    taskIds.forEach { taskId ->
      assertEmpty(submissionsManager.getSubmissionsFromMemory(setOf(taskId)))
    }
  }

  @Test
  fun `test cancel deleting submissions`() {
    configureSubmissionsResponses(submissionsLists = listOf(), submissionsDeleteRequestSuccess = true)

    val course = createEduCourse()
    val taskIds = course.allTasks.map { it.id }
    val submissionsManager = SubmissionsManager.getInstance(project)
    taskIds.forEach { taskId ->
      assertEmpty(submissionsManager.getSubmissionsFromMemory(setOf(taskId)))
      submissionsManager.addToSubmissions(taskId, generateMarketplaceSubmission())
      checkSubmissionsPresent(submissionsManager, taskId)
    }

    deleteSubmissionsWithTestDialog(object : SubmissionsDeleteDialog {
      override fun showWithResult(): Int = AdvancedSubmissionsDeleteDialog.CANCEL
    }) {
      testAction(DeleteAllSubmissionsAction.ACTION_ID)
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    taskIds.forEach { taskId ->
      checkSubmissionsPresent(submissionsManager, taskId)
    }
  }

  private fun generateMarketplaceSubmission(): MarketplaceSubmission = MarketplaceSubmission().apply {
    id = Random.nextInt()
    solutionFiles = listOf(SolutionFile("file${id}", "text${id}", true))
    status = CheckStatus.values().random().toString()
  }

  @Test
  fun `test submission created after edu task check`() {
    configureSubmissionsResponses()
    createEduCourse()
    doTestSubmissionAddedAfterTaskCheck(1, CORRECT)
  }

  @Test
  fun `test all submissions loaded`() {
    configureSubmissionsResponses(listOf(loadSubmissionsDataHasNext, loadSubmissionsData))
    createEduCourse()
    doTestSubmissionsLoaded(setOf(1, 2), mapOf(1 to 2, 2 to 2))
  }

  @Test
  fun `test solution files loaded`() {
    configureSubmissionsResponses(solutionsKeyTextMap = mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solution))
    val course = createEduCourse()
    val firstTask = course.allTasks[0]
    val solutionFilesActual = getSolutionFiles(project, firstTask)
    val firstSolutionFile = solutionFilesActual.first()
    val placeholder = AnswerPlaceholder(2, "placeholder text")
    placeholder.init(firstTask.taskFiles["src/Task.kt"]!!, false)
    firstSolutionFile.placeholders = listOf(placeholder)

    doTestSubmissionsLoaded(setOf(1, 2), mapOf(1 to 2, 2 to 1))

    val submissionsManager = SubmissionsManager.getInstance(project)

    val submission = submissionsManager.getSubmissionWithSolutionText(firstTask, 100022)
    checkNotNull(submission)
    val solutionFiles = submission.solutionFiles
    checkNotNull(solutionFiles)
    checkSolutionFiles(solutionFiles, solutionFilesActual)
  }

  @Test
  fun `test report community solution action success`() {
    configureSubmissionsResponses(reportSolutionRequestSuccess = true)
    createEduCourse()

    val taskId = Random.nextInt()
    val communitySolutions = communitySolutions(taskId)
    val submissionsManager = SubmissionsManager.getInstance(project)
    val communityIds = communitySolutions.map { it.id as Int }
    val solutionToReport = communitySolutions.random()
    val solutionToReportId = solutionToReport.id as Int
    submissionsManager.addCommunitySolutions(taskId, communitySolutions)

    checkCommunitySolutionsPresented(taskId, communityIds)
    withTestDialog(TestDialog.YES) {
      testAction(ReportCommunitySolutionAction.ACTION_ID, reportActionDataContext(project, solutionToReport, solutionToReportId))
    }
    checkCommunitySolutionsPresented(taskId, communityIds.filter { it != solutionToReportId })
    checkCommunitySolutionNotPresented(taskId, solutionToReportId)
  }

  @Test
  fun `test report community solution action failed`() {
    configureSubmissionsResponses()
    createEduCourse()

    val taskId = Random.nextInt()
    val communitySolutions = communitySolutions(taskId)
    val submissionsManager = SubmissionsManager.getInstance(project)
    val communityIds = communitySolutions.map { it.id as Int }
    val solutionToReport = communitySolutions.random()
    val solutionToReportId = solutionToReport.id as Int
    submissionsManager.addCommunitySolutions(taskId, communitySolutions)

    checkCommunitySolutionsPresented(taskId, communityIds)
    withTestDialog(TestDialog.YES) {
      testAction(ReportCommunitySolutionAction.ACTION_ID, reportActionDataContext(project, solutionToReport, solutionToReportId))
    }
    checkCommunitySolutionsPresented(taskId, communityIds)
  }

  private fun checkCommunitySolutionsPresented(taskId: Int, solutionsIds: List<Int>) {
    val solutionsIdsFromMemory = SubmissionsManager.getInstance(project).getCommunitySubmissionsFromMemory(taskId)?.mapNotNull { it.id }?.toSet()
    checkNotNull(solutionsIdsFromMemory)
    assertTrue(solutionsIds.all { solutionsIdsFromMemory.contains(it) })
  }

  private fun checkCommunitySolutionNotPresented(taskId: Int, solutionId: Int) {
    val solutionsIdsFromMemory = SubmissionsManager.getInstance(project).getCommunitySubmissionsFromMemory(taskId)?.mapNotNull { it.id }?.toSet()
    checkNotNull(solutionsIdsFromMemory)
    assertTrue(solutionId !in solutionsIdsFromMemory)
  }

  private fun checkSolutionFiles(expectedList: List<SolutionFile>, actualList: List<SolutionFile>?) {
    checkNotNull(actualList)
    assertEquals(expectedList.size, actualList.size)
    for (n in expectedList.indices) {
      val expected = expectedList[n]
      val actual = actualList[n]
      assertEquals(expected.name, actual.name)
      assertEquals(expected.isVisible, actual.isVisible)
      assertEquals(expected.text, actual.text)
      if (expected.placeholders.isNullOrEmpty()) continue
      checkNotNull(actual.placeholders)
      assertEquals(expected.placeholders!!.size, actual.placeholders!!.size)
      val expectedPlaceholder = expected.placeholders!!.first()
      val actualPlaceholder = actual.placeholders!!.first()
      assertEquals(expectedPlaceholder.placeholderText, actualPlaceholder.placeholderText)
      assertEquals(expectedPlaceholder.status, actualPlaceholder.status)
      assertEquals(expectedPlaceholder.possibleAnswer, actualPlaceholder.possibleAnswer)
      assertEquals(expectedPlaceholder.length, actualPlaceholder.length)
      assertEquals(expectedPlaceholder.placeholderDependency, actualPlaceholder.placeholderDependency)
      assertEquals(expectedPlaceholder.offset, actualPlaceholder.offset)
    }
  }

  private fun createEduCourse(): EduCourse {
    return courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task1", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse
  }

  companion object {
    const val FIRST_TASK_SUBMISSION_AWS_KEY = "11"
    const val SECOND_TASK_SUBMISSION_AWS_KEY = "12"

    fun configureSubmissionsResponses(
      submissionsLists: List<String> = listOf(loadSubmissionsData),
      solutionsKeyTextMap: Map<String, String> = emptyMap(),
      submissionsDeleteRequestSuccess: Boolean = false,
      reportSolutionRequestSuccess: Boolean = false,
      userAgreementState: UserAgreementState = UserAgreementState.ACCEPTED
    ) {
      mockkConstructor(Retrofit::class)
      val service = mockk<SubmissionsService>()
      every {
        anyConstructed<Retrofit>().create(SubmissionsService::class.java)
      } returns service

      val mapper = MarketplaceSubmissionsConnector.getInstance().objectMapper

      mockJBAccount()

      for (i in submissionsLists.indices) {
        val getAllSubmissionsPageableCall = mockk<Call<MarketplaceSubmissionsList>>()
        every { service.getAllSubmissionsForCourse(any(), i + 1) } returns getAllSubmissionsPageableCall
        val getAllSubmissionsPageableResponse = mapper.treeToValue(mapper.readTree(submissionsLists[i]), MarketplaceSubmissionsList::class.java)
        every { getAllSubmissionsPageableCall.execute() } returns Response.success(getAllSubmissionsPageableResponse)
      }

      val deleteSubmissionsCall = mockk<Call<ResponseBody>>()
      every { service.deleteAllSubmissions() } returns deleteSubmissionsCall
      every { service.deleteAllSubmissions(any()) } returns deleteSubmissionsCall
      val responseCode = if (submissionsDeleteRequestSuccess) HttpURLConnection.HTTP_NO_CONTENT else HttpURLConnection.HTTP_RESET
      every { deleteSubmissionsCall.execute() } answers {
        Response.success(responseCode, "empty response body".toResponseBody())
      }

      val postSubmissionCall = mockk<Call<MarketplaceSubmission>>()
      every { service.postSubmission(any(), any(), any(), any()) } returns postSubmissionCall
      val postSubmissionResponse = mapper.treeToValue(mapper.readTree(postSubmissionData), MarketplaceSubmission::class.java)
      every { postSubmissionCall.execute() } answers {
        Response.success(postSubmissionResponse)
      }

      val reportSolutionCall = mockk<Call<ResponseBody>>()
      every { service.reportSolution(any()) } returns reportSolutionCall
      val reportCommunityResponse = if (reportSolutionRequestSuccess) {
        Response.success(HttpURLConnection.HTTP_NO_CONTENT, "mock report response body".toResponseBody())
      }
      else {
        Response.error(HttpURLConnection.HTTP_NOT_FOUND, "mock report response body".toResponseBody())
      }
      every { reportSolutionCall.execute() } returns reportCommunityResponse

      val userAgreementStateCall = mockk<Call<ResponseBody>>()
      coEvery {  service.getUserAgreementState() } returns userAgreementStateCall
      every { userAgreementStateCall.execute() } answers {
        Response.success(userAgreementState.toString().toResponseBody("application/json; charset=UTF-8".toMediaType()))
      }

      if (solutionsKeyTextMap.isNotEmpty()) {
        mockkObject(MarketplaceSubmissionsConnector)

        for (key in solutionsKeyTextMap.keys) {
          val downloadLink = "downloadLink/key=$key"
          val getSolutionLinkCall = mockk<Call<ResponseBody>>()
          every { service.getSolutionDownloadLink(key) } returns getSolutionLinkCall
          every { getSolutionLinkCall.execute() } returns Response.success(
            downloadLink.toResponseBody("application/json; charset=UTF-8".toMediaType())
          )

          every {
            MarketplaceSubmissionsConnector.loadSolutionByLink(downloadLink)
          } returns solutionsKeyTextMap.getOrDefault(key, "no solution present for $key")
        }
      }
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

    @Language("JSON")
    private val postSubmissionData = """
      {
        "id" : 100022,
        "task_id" : 1,
        "solution_aws_key" : "11111/1/100/2023-01-12T07:55:18.061429680/11111111",
        "time" : "2023-01-12T07:55:18.06143",
        "format_version" : 13,
        "update_version" : 1,
        "status" : "correct"
      }
    """

    @Language("JSON")
    private val loadSubmissionsData = """
      {
        "has_next" : false,
        "submissions" : [
          {
            "id" : 100022,
            "task_id" : 1,
            "solution_aws_key" : $FIRST_TASK_SUBMISSION_AWS_KEY,
            "time" : "2023-01-12T07:55:18.06143",
            "format_version" : 13,
            "update_version" : 1,
            "status" : "correct"
          },
          {
            "id" : 100023,
            "task_id" : 1,
            "solution_aws_key" : $FIRST_TASK_SUBMISSION_AWS_KEY,
            "time" : "2023-01-12T07:55:18.06143",
            "format_version" : 13,
            "update_version" : 1,
            "status" : "correct"
          },
          {
            "id" : 100024,
            "task_id" : 2,
            "solution_aws_key" : $SECOND_TASK_SUBMISSION_AWS_KEY,
            "time" : "2023-01-12T07:55:18.06143",
            "format_version" : 13,
            "update_version" : 1,
            "status" : "correct"
          }
        ]
      }
  """

    @Language("JSON")
    private val loadSubmissionsDataHasNext = """
      {
        "has_next" : true,
        "submissions" : [
          {
            "id" : 100020,
            "task_id" : 2,
            "solution_aws_key" : $FIRST_TASK_SUBMISSION_AWS_KEY,
            "time" : "2023-01-12T07:55:18.06143",
            "format_version" : 13,
            "update_version" : 1,
            "status" : "correct"
          }
        ]
      }
  """

    @Language("JSON")
    private val solution = """
      [
        {
          "name" : "src/Task.kt",
          "placeholders" : [
            {
              "offset" : 2,
              "length" : 16,
              "possible_answer" : "",
              "placeholder_text" : "placeholder text"
            }
          ],
          "is_visible" : true,
          "text" : "fun foo() {}"
        },
        {
          "name" : "test/Tests1.kt",
          "placeholders" : [ ],
          "is_visible" : false,
          "text" : "fun tests1() {}"
        }
      ]
    """
  }
}