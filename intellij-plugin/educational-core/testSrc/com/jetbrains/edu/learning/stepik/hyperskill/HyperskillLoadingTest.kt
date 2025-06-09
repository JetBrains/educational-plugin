package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.junit.Test

class HyperskillLoadingTest : SolutionLoadingTestBase() {
  override fun doLoginFakeUser() = logInFakeHyperskillUser()

  override fun doLogout() = logOutFakeHyperskillUser()

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private fun configureResponse(responseFileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ -> mockResponse(responseFileName) }
  }

  @Test
  fun `test solution loading second stage failed`() {
    configureResponse("submission_stage2_failed.json")

    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test local changes are not lost`() {
    configureResponse("submission_stage2_failed.json")

    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val newText = makeLocalChanges(findFileInTask(0, 1, "src/Task.kt"))

    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", newText)
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loading first stage solved on web interface`() {
    configureResponse("submission_stage1_web.json")

    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loading code task`() {
    configureResponse("submission_code_task.json")

    val course = hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
      lesson("Problems") {
        codeTask("task1", stepId = 4) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    }

    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests1.kt", "fun tests1() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
      }
      dir("Problems") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "user code")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loaded project with problems`() {
    configureResponse("submission_code_task.json")

    val topicName = "topicName"
    hyperskillCourseWithFiles(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(topicName) {
          codeTask(stepId = 4) {
            taskFile("src/Task.kt", "fun foo() {}")
          }
        }
      }
    }

    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(getCourse())
    UIUtil.dispatchAllInvocationEvents()
    val fileTree = fileTree {
      dir(HYPERSKILL_TOPICS) {
        dir(topicName) {
          dir("task1") {
            dir("src") {
              file("Task.kt", "user code")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)

  }

  @Test
  fun `test solution loading all stages solved`() {
    configureResponse("submissions_all_solved.json")
    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests3.kt", "fun tests3() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loading with new file on the second stage`() {
    configureResponse("submission_stage2_failed_new_file.json")
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
            file("additional.txt", "additional file")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loading with new files with different visibility`() {
    configureResponse("submission_stage2_failed_new_files_with_different_visibility.json")
    val course = createHyperskillCourse()

    withVirtualFileListener(course) {
      HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }
    val task1 = course.findTask("lesson1", "task1")
    // Additional visible file "src/additional1.txt" is added, but not visible file "src/additional2.txt" should not
    checkVisibility(task1.name, task1.taskFiles,
                    mapOf("src/Task.kt" to true,
                          "src/Baz.kt" to true,
                          "test/Tests1.kt" to false,
                          "src/additional1.txt" to true))

    val task2 = course.findTask("lesson1", "task2")
    // Additional visible file "src/additional3.txt" is added, previous visible file "test/Tests2.kt" is still there
    checkVisibility(task2.name, task2.taskFiles,
                    mapOf("src/Task.kt" to true,
                          "src/Baz.kt" to true,
                          "test/Tests2.kt" to false,
                          "src/additional3.txt" to true))
  }

  @Test
  fun `test solution loading with new file on the first stage`() {
    configureResponse("submission_stage1_new_file.json")
    val course = createHyperskillCourse()
    withVirtualFileListener(course) {
      HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
            file("additional.txt", "additional file")
          }
          dir("test") {
            file("Tests1.kt", "fun tests1() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test solution loading new file from the first stage propagated`() {
    configureResponse("submission_stage1_new_file_solved.json")
    val course = createHyperskillCourse()
    withVirtualFileListener(course) {
      HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
            file("additional.txt", "additional file")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test navigation after solution loading`() {
    configureResponse("submission_stage2_failed.json")
    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      withEduTestDialog(EduTestDialog(Messages.NO)) {
        task1.openTaskFileInEditor("src/Task.kt")
        TaskToolWindowView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
        testAction(NextTaskAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = true)
      }
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests2.kt", "fun tests2() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test all topics loaded`() {
    val items = mapOf(1 to "topics_response_1.json", 2 to "topics_response_2.json")

    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      val result = TOPICS_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      items[stepId]?.let { mockResponse(it) } ?: mockResponse("response_empty.json")
    }
    val course = createHyperskillCourse()
    mockConnector.fillTopics(project, course)
    assertEquals(3, course.taskToTopics[0]?.size)
  }

  @Test
  fun `test do not apply old submissions on new user changes`() =
    doApplySubmissionOnNonCurrentTaskTest("submission_stage1_ancient_submission.json") {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun foo2() {}\nfun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests1.kt", "fun tests1() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

  @Test
  fun `test apply new submissions on old user changes`() =
    doApplySubmissionOnNonCurrentTaskTest("submission_stage1_newest_submission.json") {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun userFoo() {}")
            file("Baz.kt", "fun userBaz() {}")
          }
          dir("test") {
            file("Tests1.kt", "fun tests1() {}")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }


  /**
   * Types something in the first task, navigate to the second task,
   * applies submissions from given response and navigates back
   */
  private fun doApplySubmissionOnNonCurrentTaskTest(
    responseFileName: String,
    expectedStructure: FileTreeBuilder.() -> Unit
  ) {
    configureResponse(responseFileName)

    val course = createHyperskillCourse(completeStages = true)

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun foo2() {}\n")
      testAction(NextTaskAction.ACTION_ID)

      assertEquals(task2, course.getProjectLesson()!!.currentTask())

      HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

      task2.openTaskFileInEditor("src/Task.kt")
      testAction(PreviousTaskAction.ACTION_ID)

      assertEquals(task1, course.getProjectLesson()!!.currentTask())
    }

    fileTree(expectedStructure).assertEquals(rootDir, myFixture)
  }

  private fun createHyperskillCourse(completeStages: Boolean = false): HyperskillCourse {
    return hyperskillCourseWithFiles(completeStages = completeStages) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}", visible = false)
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun tests2() {}", visible = false)
        }
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests3.kt", "fun tests3() {}", visible = false)
        }
      }
    }
  }

  private fun checkVisibility(name: String, taskFiles: Map<String, TaskFile>, visibility: Map<String, Boolean>) {
    assertEquals("TaskFiles count is wrong for $name", visibility.size, taskFiles.size)
    taskFiles.forEach { (name, file) ->
      assertEquals("Visibility for $name differs", visibility[name], file.isVisible)
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  companion object {
    private val TOPICS_REQUEST_RE = """/api/topics?.*page=(\d*).*""".toRegex()
  }
}
