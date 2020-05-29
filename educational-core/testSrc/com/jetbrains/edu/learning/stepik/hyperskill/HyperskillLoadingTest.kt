package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.withTestDialog
import okhttp3.mockwebserver.MockResponse

class HyperskillLoadingTest : NavigationTestBase() {
  override fun setUp() {
    super.setUp()
    loginFakeUser()
  }

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private fun configureResponse(responseFileName: String) {
    mockConnector.withResponseHandler(testRootDisposable) { mockResponse(responseFileName) }
  }

  private fun mockResponse(fileName: String): MockResponse = MockResponseFactory.fromFile(getTestFile(fileName))

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

  fun `test local changes are not lost`() {
    configureResponse("submission_stage2_failed.json")

    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val file = findFileInTask(0, 1, "src/Task.kt")
    val newText = "lalala"

    // hack: timestamps don't change in tests
    runWriteAction { VfsUtil.saveText(file, newText) }
    (file as VirtualFileSystemEntry).timeStamp = System.currentTimeMillis()

    //explicitly mark as existing project
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, false)

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

  fun `test solution loading code task`() {
    configureResponse("submission_code_task.json")

    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
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
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

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

  fun `test navigation after solution loading`() {
    configureResponse("submission_stage2_failed.json")
    val course = createHyperskillCourse()
    HyperskillSolutionLoader.getInstance(project).loadAndApplySolutions(course)

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      withTestDialog(EduTestDialog(Messages.NO)) {
        task1.openTaskFileInEditor("src/Task.kt")
        myFixture.testAction(NextTaskAction())
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

  fun `test all topics loaded`() {
    val items = mapOf(1 to "topics_response_1.json", 2 to "topics_response_2.json")

    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val result = TOPICS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      val stepId = result.groupValues[1].toInt()
      items[stepId]?.let { mockResponse(it) } ?: mockResponse("response_empty.json")
    }
    val course = createHyperskillCourse()
    mockConnector.fillTopics(course, project)
    assertEquals(3, course.taskToTopics[0]?.size)
  }

  private fun createHyperskillCourse(): HyperskillCourse {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun tests2() {}")
        }
        eduTask("task3", stepId = 3) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests3.kt", "fun tests3() {}")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1), HyperskillStage(2, "", 2), HyperskillStage(3, "", 3))
    return course
  }


  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  private fun getTestFile(fileName: String) = testDataPath + fileName

  companion object {
    private val TOPICS_REQUEST_RE = """/api/topics?.*page=(\d*).*""".toRegex()
  }
}
