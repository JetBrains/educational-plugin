package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.SolutionLoadingTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoadingTest.Companion.getConfiguredSubmissionsList
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoadingTest.Companion.solutionCorrect
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoadingTest.Companion.solutionWrong
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.FIRST_TASK_SUBMISSION_AWS_KEY
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.SECOND_TASK_SUBMISSION_AWS_KEY
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.configureSubmissionsResponses
import com.jetbrains.edu.learning.submissions.SubmissionSettings

import org.intellij.lang.annotations.Language
import org.junit.Test

class MarketplaceStateOnCloseLoadingTest : SolutionLoadingTestBase() {

  override fun setUp() {
    super.setUp()

    mockJBAccount(testRootDisposable)
    val oldValue = SubmissionSettings.getInstance(project).stateOnClose
    SubmissionSettings.getInstance(project).stateOnClose = true
    Disposer.register(testRootDisposable) {
      SubmissionSettings.getInstance(project).stateOnClose = oldValue
    }
  }

  @Test
  fun `test state on close applied no submissions provided`() {
    configureSubmissionsResponses(
      listOf(submissionEmptyList),
      statesList,
      solutionsKeyTextMap = mapOf(
        FIRST_TASK_STATE_ON_CLOSE_AWS_KEY to stateOnCloseTask1,
        SECOND_TASK_STATE_ON_CLOSE_AWS_KEY to stateOnCloseTask2
      )
    )
    val course = createMarketplaceCourse()
    withVirtualFileListener(course) {
      MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "state on close text 1")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "state on close text 2")
          }
          dir("test") {
            file("Tests.kt", "fun tests()2 {}")
          }
          dir("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Unchecked, CheckStatus.Unchecked))
  }

  @Test
  fun `test state on close with submissions provided applied`() {
    configureSubmissionsResponses(
      getConfiguredSubmissionsList(firstTaskId = 1, secondTaskId = 2),
      statesList,
      solutionsKeyTextMap = mapOf(
        FIRST_TASK_SUBMISSION_AWS_KEY to solutionCorrect,
        SECOND_TASK_SUBMISSION_AWS_KEY to solutionWrong,
        FIRST_TASK_STATE_ON_CLOSE_AWS_KEY to stateOnCloseTask1,
        SECOND_TASK_STATE_ON_CLOSE_AWS_KEY to stateOnCloseTask2
      )
    )
    val course = createMarketplaceCourse()
    withVirtualFileListener(course) {
      MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "text from submission solved")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "state on close text 2")
          }
          dir("test") {
            file("Tests.kt", "fun tests()2 {}")
          }
          dir("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Solved, CheckStatus.Failed))
  }

  private fun createMarketplaceCourse(courseVersion: Int = 1) = courseWithFiles(
    language = FakeGradleBasedLanguage, courseProducer = ::EduCourse, id = 1
  ) {
    lesson("lesson1") {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "fun foo() {}")
        taskFile("test/Tests.kt", "fun tests() {}")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "fun foo()2 {}")
        taskFile("test/Tests.kt", "fun tests()2 {}")
      }
    }
  }.apply {
    isMarketplace = true
    marketplaceCourseVersion = courseVersion
  } as EduCourse

  companion object {
    private const val FIRST_TASK_STATE_ON_CLOSE_AWS_KEY = "22"
    private const val SECOND_TASK_STATE_ON_CLOSE_AWS_KEY = "23"
  }

    @Language("JSON") private val statesList = listOf("""
      {
        "has_next" : false,
        "states_on_close" : [
            {
              "id" : 100022,
              "task_id" : 1,
              "solution_aws_key" : $FIRST_TASK_STATE_ON_CLOSE_AWS_KEY,
              "time" : "2022-01-12T07:55:18.06143",
              "format_version" : 13,
              "update_version" : 1
            },
            {
              "id" : 100023,
              "task_id" : 2,
              "solution_aws_key" : $SECOND_TASK_STATE_ON_CLOSE_AWS_KEY,
              "time" : "2023-02-12T07:55:18.06143",
              "format_version" : 13,
              "update_version" : 1
            }
          ]
      }
  """)

  @Language("JSON")
  private val stateOnCloseTask1 = """
  [
    {
      "name" : "src/Task.kt",
      "placeholders" : [
        {
          "offset" : 181,
          "length" : 41,
          "possible_answer" : "possible answer",
          "placeholder_text" : "placeholder text"
        }
      ],
      "is_visible" : true,
      "text" : "state on close text 1"
    },
    {
      "name" : "test/Tests.kt",
      "placeholders" : [ ],
      "is_visible" : false,
      "text" : ""
    }
  ]
    """

  @Language("JSON")
  private val stateOnCloseTask2 = """
  [
    {
      "name" : "src/Task.kt",
      "placeholders" : [
        {
          "offset" : 181,
          "length" : 41,
          "possible_answer" : "possible answer",
          "placeholder_text" : "placeholder text"
        }
      ],
      "is_visible" : true,
      "text" : "state on close text 2"
    },
    {
      "name" : "test/Tests.kt",
      "placeholders" : [ ],
      "is_visible" : false,
      "text" : ""
    }
  ]
    """

  @Language("JSON")
  val submissionEmptyList = """
    {
      "has_next" : false,
      "submissions" :
        []
    }
  """
}