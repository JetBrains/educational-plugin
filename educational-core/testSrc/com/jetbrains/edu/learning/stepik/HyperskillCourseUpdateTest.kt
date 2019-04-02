package com.jetbrains.edu.learning.stepik

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.io.storage.AbstractStorage
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillCourseUpdater
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*

class HyperskillCourseUpdateTest : EduTestCase() {

  override fun tearDown() {
    AbstractStorage.deleteFiles(FrameworkLessonManagerImpl.constructStoragePath(project))
    super.tearDown()
  }

  private lateinit var course: HyperskillCourse

  fun `test update`() {
    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        descriptionText = "New Description"
      }
      taskList[1].apply {
        updateDate = Date(1000)
        descriptionFormat = DescriptionFormat.MD
      }
    }

    val tasks = course.lessons[0].taskList
    assertEquals(Date(100), tasks[0].updateDate)
    assertEquals(Date(1000), tasks[1].updateDate)
    assertEquals("New Description", tasks[0].descriptionText)
    assertEquals(DescriptionFormat.MD, tasks[1].descriptionFormat)
  }

  fun `test update unmodified current task`() {
    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.setText(taskText)
        taskFiles["test/Tests1.kt"]!!.setText(testText)
      }
    }

    val task = course.taskList[0]
    assertEquals(taskText, task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests1.kt"]!!.text)

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", taskText)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests1.kt", testText)
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update modified current task`() {
    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()

    val task = course.taskList[0]
    task.openTaskFileInEditor("src/Task.kt")
    myFixture.type("fun bar() {}\n")

    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.setText(taskText)
        taskFiles["test/Tests1.kt"]!!.setText(testText)
      }
    }

    assertEquals("fun foo() {}", task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests1.kt"]!!.text)

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """.trimIndent())
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests1.kt", testText)
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update solved current task`() {
    val task = course.taskList[0]
    task.status = CheckStatus.Solved

    updateCourse {
      taskList[0].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.setText("fun foo2() {}")
        taskFiles["test/Tests1.kt"]!!.setText("""
          fun test1() {}
          fun test2() {}
        """.trimIndent())
      }
    }

    assertEquals("fun foo() {}", task.taskFiles["src/Task.kt"]!!.text)
    assertEquals("fun test1() {}", task.taskFiles["test/Tests1.kt"]!!.text)

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests1.kt", "fun test1() {}")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update unmodified non current task`() {
    val task1 = course.taskList[0]
    val task2 = course.taskList[1]

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    updateCourse {
      taskList[1].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.setText(taskText)
        taskFiles["test/Tests2.kt"]!!.setText(testText)
      }
    }

    assertEquals("fun foo() {}", task2.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task2.taskFiles["test/Tests2.kt"]!!.text)

    try {
    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }
    } catch (e: Exception) {
      throw  e
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", testText)
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update modified non current task`() {
    val task1 = course.taskList[0]
    val task2 = course.taskList[1]

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.testAction(PreviousTaskAction())
    }

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    updateCourse {
      taskList[1].apply {
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.setText(taskText)
        taskFiles["test/Tests2.kt"]!!.setText(testText)
      }
    }

    assertEquals("fun foo() {}", task2.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task2.taskFiles["test/Tests2.kt"]!!.text)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.testAction(NextTaskAction())
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", "fun bar() {}\nfun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", testText)
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update additional files`() {
    val buildGradleText = """
      apply plugin: "java"
      sourceCompatibility = '1.8'
    """.trimIndent()
    updateCourse {
      additionalFiles.add(TaskFile("build.gradle", buildGradleText))
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
        }
      }
      file("build.gradle", buildGradleText)
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  private fun updateCourse(changeCourse: HyperskillCourse.() -> Unit) {
    val element = XmlSerializer.serialize(course)
    val remoteCourse = XmlSerializer.deserialize(element, HyperskillCourse::class.java)
    remoteCourse.init(null, null, false)
    remoteCourse.changeCourse()
    HyperskillCourseUpdater.updateCourse(project, course, remoteCourse)
  }

  override fun createCourse() {
    course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")

        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
  }
}

private val HyperskillCourse.taskList: List<Task> get() = lessons[0].taskList
