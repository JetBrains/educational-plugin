package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.SolutionLoadingTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.WRONG
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.FIRST_TASK_SUBMISSION_AWS_KEY
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.SECOND_TASK_SUBMISSION_AWS_KEY
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.configureSubmissionsResponses
import org.intellij.lang.annotations.Language
import org.junit.Test

class MarketplaceSolutionLoadingTest : SolutionLoadingTestBase() {

  @Test
  fun `test solution not applied at submission and course versions incompatibility`() {
    configureSubmissionsResponses(getConfiguredSubmissionsList(),
                                  mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionCorrect, SECOND_TASK_SUBMISSION_AWS_KEY to solutionCorrect))
    val course = createMarketplaceCourse(2)
    MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "fun foo()2 {}")
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
  }

  @Test
  fun `test local changes are not lost`() {
    configureSubmissionsResponses(getConfiguredSubmissionsList(),
                                  mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionCorrect, SECOND_TASK_SUBMISSION_AWS_KEY to solutionWrong))
    val course = createMarketplaceCourse()
    MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val newText = makeLocalChanges(findFileInTask(0, 0, "src/Task.kt"))

    MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", newText)
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "text from submission failed")
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
  }

  @Test
  fun `test theory task check status solved applied`() {
    configureSubmissionsResponses(listOf(submissionForTheoryTask))

    val course = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse, id = 1) {
      lesson("lesson1") {
        theoryTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("test/Tests.kt", "fun tests() {}")
        }
      }
    }.apply { isMarketplace = true } as EduCourse

    withVirtualFileListener(course) {
      MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Solved))
  }

  @Test
  fun `test solution loading first task solved second task failed`() {
    configureSubmissionsResponses(getConfiguredSubmissionsList(),
                                  mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionCorrect, SECOND_TASK_SUBMISSION_AWS_KEY to solutionWrong))
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
            file("Task.kt", "text from submission failed")
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

  @Test
  fun `test solution loading first task failed second task solved`() {
    configureSubmissionsResponses(getConfiguredSubmissionsList(firstStatus = WRONG, secondStatus = CORRECT),
                                  mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionWrong, SECOND_TASK_SUBMISSION_AWS_KEY to solutionCorrect))
    val course = createMarketplaceCourse()
    withVirtualFileListener(course) {
      MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "text from submission failed")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "text from submission solved")
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

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Failed, CheckStatus.Solved))
  }

  @Test
  fun `test first submission from the list applied`() {
    configureSubmissionsResponses(
      getConfiguredSubmissionsList(firstTaskId = 1, secondTaskId = 1, firstStatus = CORRECT, secondStatus = WRONG),
      mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionCorrect, SECOND_TASK_SUBMISSION_AWS_KEY to solutionWrong))
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
            file("Task.kt", "fun foo()2 {}")
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

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Solved, CheckStatus.Unchecked))
  }

  @Test
  fun `test solution for output task applied`() {
    configureSubmissionsResponses(getConfiguredSubmissionsList(),
      mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionCorrect, SECOND_TASK_SUBMISSION_AWS_KEY to solutionWrong))

    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse, id = 1
    ) {
      lesson("lesson1") {
        outputTask("task1", stepId = 1) {
          kotlinTaskFile("src/Task.kt", """
          fun main() {
              println("OK")
          }
        """)
          taskFile("test/output.txt") {
            withText("OK!")
          }
        }
        outputTask("task2", stepId = 2) {
          kotlinTaskFile("src/Task.kt", """
          fun main() {
              print("OK")
          }
        """)
          taskFile("test/output.txt") {
            withText("OK!")
          }
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 1
    } as EduCourse
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
            file("Tests.kt", "")
            file("output.txt", "OK!")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "text from submission failed")
          }
          dir("test") {
            file("Tests.kt", "")
            file("output.txt", "OK!")
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
  @Test
  fun `test solution loading with additional file in the first task`() {
    configureSubmissionsResponses(getConfiguredSubmissionsList(firstStatus = CORRECT, secondStatus = CORRECT),
                                  mapOf(FIRST_TASK_SUBMISSION_AWS_KEY to solutionWithAdditionalFile,
                                        SECOND_TASK_SUBMISSION_AWS_KEY to solutionCorrect))
    val course = createMarketplaceCourse()
    withVirtualFileListener(course) {
      MarketplaceSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "text from submission solved")
            file("additional_file.txt", "additional file text")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          dir("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "text from submission solved")
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

    checkTaskStatuses(course.allTasks, listOf(CheckStatus.Solved, CheckStatus.Solved))
  }

  private fun createMarketplaceCourse(courseVersion: Int = 1) = courseWithFiles(language = FakeGradleBasedLanguage,
                                                                                courseProducer = ::EduCourse, id = 1) {
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

  private fun getConfiguredSubmissionsList(firstTaskId: Int = 1,
                                           secondTaskId: Int = 2,
                                           firstStatus: String = CORRECT,
                                           secondStatus: String = WRONG): List<String> {
    @Language("JSON")
    val submissionsList = """
      {
        "has_next" : false,
        "submissions" : [
            {
              "id" : 100022,
              "task_id" : $firstTaskId,
              "solution_aws_key" : $FIRST_TASK_SUBMISSION_AWS_KEY,
              "time" : "2023-01-12T07:55:18.06143",
              "format_version" : 13,
              "update_version" : 1,
              "status" : "$firstStatus"
            },
            {
              "id" : 100023,
              "task_id" : $secondTaskId,
              "solution_aws_key" : $SECOND_TASK_SUBMISSION_AWS_KEY,
              "time" : "2023-01-12T07:55:18.06143",
              "format_version" : 13,
              "update_version" : 1,
              "status" : "$secondStatus"
            }
          ]
      }
  """
    return listOf(submissionsList)
  }

  @Language("JSON")
  private val submissionForTheoryTask = """
    {
      "has_next" : false,
      "submissions" :
        [
          {
            "id" : 100022,
            "task_id" : 1,
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
  private val solutionWithAdditionalFile = """
  [
    {
      "name" : "src/Task.kt",
      "placeholders" : [ ],
      "is_visible" : true,
      "text" : "text from submission solved"
    },
    {
      "name" : "test/Tests.kt",
      "placeholders" : [ ],
      "is_visible" : false,
      "text" : ""
    },
    {
      "name" : "src/additional_file.txt",
      "placeholders" : [ ],
      "is_visible" : true,
      "text" : "additional file text"
    }
  ]
"""

  @Language("JSON")
  private val solutionCorrect = """
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
      "text" : "text from submission solved"
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
  private val solutionWrong = """
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
      "text" : "text from submission failed"
    },
    {
      "name" : "test/Tests.kt",
      "placeholders" : [],
      "is_visible" : false,
      "text" : ""
    }
  ]
"""
}