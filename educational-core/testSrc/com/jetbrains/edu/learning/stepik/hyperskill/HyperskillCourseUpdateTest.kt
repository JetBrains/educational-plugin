package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.actions.navigate.NavigationTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillCourseUpdater.Companion.shouldBeUpdated
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*

class HyperskillCourseUpdateTest : NavigationTestBase() {

  private lateinit var course: HyperskillCourse

  fun `test update`() {
    createHyperskillCourse()

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
    createHyperskillCourse()

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
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update modified current task`() {
    createHyperskillCourse()

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
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update solved current task`() {
    createHyperskillCourse()

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
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update unmodified non current task`() {
    createHyperskillCourse()

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
    }
    catch (e: Exception) {
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
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update modified non current task`() {
    createHyperskillCourse()

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
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test update additional files`() {
    createHyperskillCourse()

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
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle", buildGradleText)
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  fun `test task description updated`() {
    createHyperskillCourse()

    val newDescription = "new description"
    updateCourse {
      taskList[0].apply {
        descriptionText = newDescription
        updateDate = Date(100)
      }
    }

    checkDescriptionUpdated(findTask(0, 0), newDescription)
  }

  private fun checkDescriptionUpdated(task: Task, @Suppress("SameParameterValue") newDescription: String) {
    val taskDescription = EduUtils.getTaskTextFromTask(project, task)!!
    assertTrue("Task Description not updated", taskDescription.contains(newDescription))
  }

  fun `test coding tasks updated`() {
    val taskFileName = "Task.txt"
    val oldText = "old text"
    val newText = "new text"

    course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        codeTask(taskDescription = oldText) {
          taskFile(taskFileName, oldText)
        }
        codeTask(taskDescription = oldText) {
          taskFile(taskFileName, oldText)
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    findTask(0, 0).status = CheckStatus.Solved

    updateCourse(findLesson(0).taskList.map { it.toTaskUpdate {
      descriptionText = newText
      updateDate = Date(100)
      getTaskFile(taskFileName)!!.setText(newText)
    } })

    fileTree {
      dir("Problems") {
        dir("task1") {
          file(taskFileName, oldText)
          file("task.html", newText)
        }
        dir("task2") {
          file(taskFileName, newText)
          file("task.html", newText)
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  private fun Task.toTaskUpdate(changeTask: Task.() -> Unit): HyperskillCourseUpdater.TaskUpdate {
    val remoteTask = this.copy()
    remoteTask.changeTask()
    return HyperskillCourseUpdater.TaskUpdate(this, remoteTask)
  }

  private fun updateCourse(codeChallengesUpdates: List<HyperskillCourseUpdater.TaskUpdate> = emptyList(),
                           changeCourse: (HyperskillCourse.() -> Unit)? = null) {
    val remoteCourse = changeCourse?.let { toRemoteCourse(changeCourse) }
    HyperskillCourseUpdater(project, course).doUpdate(remoteCourse, codeChallengesUpdates)
    val isProjectUpToDate = remoteCourse == null || course.getProjectLesson()?.shouldBeUpdated(project, remoteCourse) == false
    assertTrue("Project is not up-to-date after update", isProjectUpToDate)
  }

  private fun toRemoteCourse(changeCourse: HyperskillCourse.() -> Unit): HyperskillCourse {
    val element = XmlSerializer.serialize(course)
    val remoteCourse = XmlSerializer.deserialize(element, HyperskillCourse::class.java)
    remoteCourse.getProblemsLesson()?.let { remoteCourse.removeLesson(it) }
    remoteCourse.init(null, null, false)
    remoteCourse.changeCourse()
    return remoteCourse
  }

  private fun createHyperskillCourse() {
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
    course.hyperskillProject = HyperskillProject()
  }
}

private val HyperskillCourse.taskList: List<Task> get() = lessons[0].taskList
