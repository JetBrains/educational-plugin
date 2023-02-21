package com.jetbrains.edu.learning.marketplace

import com.intellij.ui.JBAccountInfoService
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.api.*
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.withFeature
import io.mockk.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.intellij.lang.annotations.Language
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class MarketplaceSubmissionsTest : SubmissionsTestBase() {

  override fun setUp() {
    super.setUp()
    loginFakeMarketplaceUser()
  }

  fun `test submission created after edu task check`() {
    configureSubmissionsResponses()
    createEduCourse()
    doTestSubmissionAddedAfterTaskCheck(1, CORRECT)
  }

  fun `test all submissions loaded`() {
    configureSubmissionsResponses(listOf(loadSubmissionsDataHasNext, loadSubmissionsData))
    createEduCourse()
    doTestSubmissionsLoaded(setOf(1, 2), mapOf(1 to 2, 2 to 2))
  }

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

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS, true) {
      super.runTestRunnable(context)
    }
  }

  companion object {
    const val FIRST_TASK_SUBMISSION_AWS_KEY = "11"
    const val SECOND_TASK_SUBMISSION_AWS_KEY = "12"

    fun configureSubmissionsResponses(submissionsLists: List<String> = listOf(loadSubmissionsData),
                                      solutionsKeyTextMap: Map<String, String> = emptyMap()) {
      mockkConstructor(Retrofit::class)
      val service = mockk<SubmissionsService>()
      every {
        anyConstructed<Retrofit>().create(SubmissionsService::class.java)
      } returns service

      val mapper = MarketplaceSubmissionsConnector.getInstance().objectMapper

      mockkStatic(JBAccountInfoService::class)
      every { JBAccountInfoService.getInstance()?.accessToken } returns object : Future<String?> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
          return false
        }

        override fun isCancelled(): Boolean {
          return false
        }

        override fun isDone(): Boolean {
          return true
        }

        override fun get(): String? {
          return null
        }

        override fun get(timeout: Long, unit: TimeUnit): String {
          return "test token"
        }
      }

      for (i in submissionsLists.indices) {
        val getAllSubmissionsPageableCall = mockk<Call<MarketplaceSubmissionsList>>()
        every { service.getAllSubmissionsForCourse(any(), i + 1) } returns getAllSubmissionsPageableCall
        val getAllSubmissionsPageableResponse = mapper.treeToValue(mapper.readTree(submissionsLists[i]), MarketplaceSubmissionsList::class.java)
        every { getAllSubmissionsPageableCall.execute() } returns Response.success(getAllSubmissionsPageableResponse)
      }

      val postSubmissionCall = mockk<Call<MarketplaceSubmission>>()
      every { service.postSubmission(any(), any(), any(), any()) } returns postSubmissionCall
      val postSubmissionResponse = mapper.treeToValue(mapper.readTree(postSubmissionData), MarketplaceSubmission::class.java)
      every { postSubmissionCall.execute() } returns Response.success(postSubmissionResponse)

      if (solutionsKeyTextMap.isNotEmpty()) {
        mockkObject(MarketplaceSubmissionsConnector.Companion)

        for (key in solutionsKeyTextMap.keys) {
          val downloadLink = "downloadLink/key=$key"
          val getSolutionLinkCall = mockk<Call<ResponseBody>>()
          every { service.getSolutionDownloadLink(key) } returns getSolutionLinkCall
          every { getSolutionLinkCall.execute() } returns Response.success(
            ResponseBody.create("application/json; charset=UTF-8".toMediaType(), downloadLink))

          every {
            MarketplaceSubmissionsConnector.loadSolutionByLink(downloadLink)
          } returns solutionsKeyTextMap.getOrDefault(key, "no solution present for $key")
        }
      }
    }

    @Language("JSON")
    private val postSubmissionData = """
      {
        "id" : 100022,
        "task_id" : 1,
        "solution_aws_key" : "11111/1/100/2023-01-12T07:55:18.061429680/11111111",
        "time" : "2023-01-12T07:55:18.06143",
        "format_version" : 13,
        "update_version" : 1,
        "status" : "correct",
        "checker_output" : null
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
            "status" : "correct",
            "checker_output" : null
          },
          {
            "id" : 100023,
            "task_id" : 1,
            "solution_aws_key" : $FIRST_TASK_SUBMISSION_AWS_KEY,
            "time" : "2023-01-12T07:55:18.06143",
            "format_version" : 13,
            "update_version" : 1,
            "status" : "correct",
            "checker_output" : null
          },
          {
            "id" : 100024,
            "task_id" : 2,
            "solution_aws_key" : $SECOND_TASK_SUBMISSION_AWS_KEY,
            "time" : "2023-01-12T07:55:18.06143",
            "format_version" : 13,
            "update_version" : 1,
            "status" : "correct",
            "checker_output" : null
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
            "status" : "correct",
            "checker_output" : null
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