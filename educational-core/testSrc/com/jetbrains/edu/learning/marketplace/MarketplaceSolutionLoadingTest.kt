package com.jetbrains.edu.learning.marketplace

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.MarketplaceSubmissionsTest.Companion.configureSubmissionsResponses
import org.intellij.lang.annotations.Language

class MarketplaceSolutionLoadingTest : SolutionLoadingTestBase() {
  override fun doLoginFakeUser() = loginFakeMarketplaceUser()

  override fun doLogout() = logoutFakeMarketplaceUser()

  fun `test solution not applied at submission and course versions incompatibility`() {
    configureSubmissionsResponses(listOf(submissionContent1Solved, submissionContent2Failed), versions)
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

  fun `test local changes are not lost`() {
    configureSubmissionsResponses(listOf(submissionContent1Solved, submissionContent2Failed), versions)
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

  fun `test theory task check status solved applied`() {
    configureSubmissionsResponses(listOf(submissionContent1Solved), versions)
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
    ) {
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

  fun `test solution loading first task solved second task failed`() {
    configureSubmissionsResponses(listOf(submissionContent1Solved, submissionContent2Failed), versions)
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

  fun `test solution loading first task failed second task solved`() {
    configureSubmissionsResponses(listOf(submissionContent1Failed, submissionContent2Solved), versions)
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

  fun `test solution loading with new file in the first task`() {
    configureSubmissionsResponses(listOf(submissionContent1SolvedWithAdditionalFile, submissionContent2Solved), versions)
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

  private fun createMarketplaceCourse(courseVersion: Int = 1) =
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse,
      id = 1
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

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withFeature(EduExperimentalFeatures.MARKETPLACE_SUBMISSIONS, true) {
      super.runTestRunnable(context)
    }
  }
}

//format with a bug, will be fixed on grazie side
@Language("JSON")
private val submissionContent1Solved = """
  {
    "content": "{\"content\": \"{\\\"id\\\":1,\\\"time\\\":1644312602091,\\\"status\\\":\\\"correct\\\",\\\"course_version\\\":1,\\\"task_id\\\":1,\\\"solution\\\":[{\\\"name\\\":\\\"src/Task.kt\\\",\\\"text\\\":\\\"text from submission solved\\\",\\\"is_visible\\\":true,\\\"placeholders\\\":[{\\\"offset\\\":22,\\\"length\\\":8,\\\"dependency\\\":null,\\\"possible_answer\\\":\\\"\\\\\\\"OK\\\\\\\"\\\",\\\"placeholder_text\\\":\\\"TODO()\\\",\\\"selected\\\":true}]}],\\\"version\\\":$JSON_FORMAT_VERSION}\" }"
  }
  """

@Language("JSON")
private val submissionContent1SolvedWithAdditionalFile = """
  {
    "content": "{\"content\": \"{\\\"id\\\":1,\\\"time\\\":1644312602091,\\\"status\\\":\\\"correct\\\",\\\"course_version\\\":1,\\\"task_id\\\":1,\\\"solution\\\":[{\\\"name\\\":\\\"src/Task.kt\\\",\\\"text\\\":\\\"text from submission solved\\\",\\\"is_visible\\\":true,\\\"placeholders\\\":[{\\\"offset\\\":22,\\\"length\\\":8,\\\"dependency\\\":null,\\\"possible_answer\\\":\\\"\\\\\\\"OK\\\\\\\"\\\",\\\"placeholder_text\\\":\\\"TODO()\\\",\\\"selected\\\":true}]}, {\\\"visible\\\":true,\\\"name\\\":\\\"src/additional_file.txt\\\",\\\"text\\\":\\\"additional file text\\\",\\\"is_visible\\\":true}],\\\"version\\\":$JSON_FORMAT_VERSION}\" }"
  }
  """

@Language("JSON")
private val submissionContent1Failed = """
  {
    "content": "{\"content\": \"{\\\"id\\\":1,\\\"time\\\":1644312602091,\\\"status\\\":\\\"wrong\\\",\\\"course_version\\\":1,\\\"task_id\\\":1,\\\"solution\\\":[{\\\"name\\\":\\\"src/Task.kt\\\",\\\"text\\\":\\\"text from submission failed\\\",\\\"is_visible\\\":true,\\\"placeholders\\\":[{\\\"offset\\\":22,\\\"length\\\":8,\\\"dependency\\\":null,\\\"possible_answer\\\":\\\"\\\\\\\"OK\\\\\\\"\\\",\\\"placeholder_text\\\":\\\"TODO()\\\",\\\"selected\\\":true}]}],\\\"version\\\":$JSON_FORMAT_VERSION}\" }"
  }
  """

@Language("JSON")
private val submissionContent2Solved = """
  {
    "content": "{\"content\": \"{\\\"id\\\":1,\\\"time\\\":1644312602091,\\\"status\\\":\\\"correct\\\",\\\"course_version\\\":1,\\\"task_id\\\":2,\\\"solution\\\":[{\\\"name\\\":\\\"src/Task.kt\\\",\\\"text\\\":\\\"text from submission solved\\\",\\\"is_visible\\\":true,\\\"placeholders\\\":[{\\\"offset\\\":22,\\\"length\\\":8,\\\"dependency\\\":null,\\\"possible_answer\\\":\\\"\\\\\\\"OK\\\\\\\"\\\",\\\"placeholder_text\\\":\\\"TODO()\\\",\\\"selected\\\":true}]}],\\\"version\\\":$JSON_FORMAT_VERSION}\" }"
  }
  """

@Language("JSON")
private val submissionContent2Failed = """
  {
    "content": "{\"content\": \"{\\\"id\\\":1,\\\"time\\\":1644312602091,\\\"status\\\":\\\"wrong\\\",\\\"course_version\\\":1,\\\"task_id\\\":2,\\\"solution\\\":[{\\\"name\\\":\\\"src/Task.kt\\\",\\\"text\\\":\\\"text from submission failed\\\",\\\"is_visible\\\":true,\\\"placeholders\\\":[{\\\"offset\\\":22,\\\"length\\\":8,\\\"dependency\\\":null,\\\"possible_answer\\\":\\\"\\\\\\\"OK\\\\\\\"\\\",\\\"placeholder_text\\\":\\\"TODO()\\\",\\\"selected\\\":true}]}],\\\"version\\\":$JSON_FORMAT_VERSION}\" }"
  }
  """

@Language("JSON")
private val versions = """
{
  "versions": [
    {
      "id": "SlARMo.y2SqpGLAoFHHGxjNxKqo3PRf5",
      "timestamp": 1626965426
    }
  ]
}
  """
