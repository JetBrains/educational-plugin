package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.copy
import com.jetbrains.edu.learning.courseFormat.ext.getTaskText
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillCourseWithFiles
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.FrameworkLessonsUpdateTest
import com.jetbrains.edu.learning.update.elements.TaskUpdateInfo
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES

class HyperskillFrameworkLessonsUpdateTest : FrameworkLessonsUpdateTest<HyperskillCourse>() {

  @Test
  fun `test update modified non current task`() {
    createCourseWithFrameworkLessons()

    val task1 = localCourse.taskList[0]
    val task2 = localCourse.taskList[1]

    runInEdtAndWait {
      withVirtualFileListener(localCourse) {
        task1.openTaskFileInEditor("src/Task.kt")
        myFixture.type("fun bar() {}\n")
        testAction(NextTaskAction.ACTION_ID)
        task2.openTaskFileInEditor("src/Task.kt")
        testAction(PreviousTaskAction.ACTION_ID)
      }
    }

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    val remoteCourse = toRemoteCourse {
      taskList[1].apply {
        updateDate = Date(MINUTES.toMillis(2))
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests2.kt"]!!.text = testText
      }
    }
    updateCourse(remoteCourse)

    assertEquals("fun foo() {}", task2.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task2.taskFiles["test/Tests2.kt"]!!.text)

    runInEdtAndWait {
      withVirtualFileListener(localCourse) {
        task1.openTaskFileInEditor("src/Task.kt")
        testAction(NextTaskAction.ACTION_ID)
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
  }

  @Test
  fun `test update additional files`() {
    createCourseWithFrameworkLessons()

    val buildGradleText = """
      apply plugin: "java"
      sourceCompatibility = '1.8'
    """.trimIndent()
    val remoteCourse = toRemoteCourse {
      additionalFiles = listOf(TaskFile("build.gradle", buildGradleText))
    }
    updateCourse(remoteCourse)

    runInEdtAndWait {
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
  }

  @Test
  fun `test task description updated`() {
    createCourseWithFrameworkLessons()

    val newDescription = "new description"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        descriptionText = newDescription
        updateDate = Date(MINUTES.toMillis(2))
      }
    }
    updateCourse(remoteCourse)

    checkDescriptionUpdated(findTask(0, 0), newDescription)
  }

  private fun checkDescriptionUpdated(task: Task, @Suppress("SameParameterValue") newDescription: String) {
    val taskDescription = task.getTaskText(project)!!
    assertTrue("Task Description not updated", taskDescription.contains(newDescription))
  }

  @Test
  fun `test coding tasks updated`() {
    val taskFileName = "Task.txt"
    val oldText = "old text"
    val newText = "new text"

    localCourse = courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        codeTask(taskDescription = oldText) {
          taskFile(taskFileName, oldText)
        }
        codeTask(taskDescription = oldText) {
          taskFile(taskFileName, oldText)
        }
      }
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    findTask(0, 0).status = CheckStatus.Solved

    updateCourseWithProblems(findLesson(0).taskList.map {
      it.toTaskUpdate {
        descriptionText = newText
        updateDate = Date(MINUTES.toMillis(2))
        getTaskFile(taskFileName)!!.text = newText
      }
    })

    runInEdtAndWait {
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
  }

  @Test
  fun `test project with code problems updated`() {
    val taskFileName = "Task.txt"
    val oldText = "old text"
    val newText = "new text"
    val topic = "topic"

    localCourse = hyperskillCourseWithFiles(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(topic) {
          codeTask(taskDescription = oldText) {
            taskFile(taskFileName, oldText)
          }
        }
      }
    }

    updateCourseWithProblems(localCourse.getTopicsSection()!!.getLesson(topic)!!.taskList.map {
      it.toTaskUpdate {
        descriptionText = newText
        updateDate = Date(MINUTES.toMillis(2))
        getTaskFile(taskFileName)!!.text = newText
      }
    })

    runInEdtAndWait {
      fileTree {
        dir(HYPERSKILL_TOPICS) {
          dir("topic") {
            dir("task1") {
              file(taskFileName, newText)
              file("task.html", newText)
            }
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  @Test
  fun `test project with edu problems updated`() {
    val taskFileName = "Task.txt"
    val oldText = "old text"
    val newText = "new text"
    val topic = "topic"

    localCourse = hyperskillCourseWithFiles(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(topic) {
          eduTask(taskDescription = oldText) {
            taskFile(taskFileName, oldText)
            taskFile("build.gradle")
          }
        }
      }
    }

    updateCourseWithProblems(localCourse.getTopicsSection()!!.getLesson(topic)!!.taskList.map {
      it.toTaskUpdate {
        descriptionText = newText
        updateDate = Date(MINUTES.toMillis(2))
        getTaskFile(taskFileName)!!.text = newText
      }
    })

    runInEdtAndWait {
      fileTree {
        dir(HYPERSKILL_TOPICS) {
          dir("topic") {
            dir("task1") {
              file(taskFileName, newText)
              file("task.html", newText)
              file("build.gradle")
            }
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  @Test
  fun `test project with data problems updated`() {
    val taskFileName = "Task.txt"
    val oldText = "old text"
    val newText = "new text"
    val topic = "topic"

    localCourse = hyperskillCourseWithFiles(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(topic) {
          dataTask(taskDescription = oldText) {
            taskFile(taskFileName, oldText)
          }
        }
      }
    }

    updateCourseWithProblems(localCourse.getTopicsSection()!!.getLesson(topic)!!.taskList.map {
      it.toTaskUpdate {
        descriptionText = newText
        updateDate = Date(MINUTES.toMillis(2))
        getTaskFile(taskFileName)!!.text = newText
      }
    })

    runInEdtAndWait {
      fileTree {
        dir(HYPERSKILL_TOPICS) {
          dir("topic") {
            dir("task1") {
              file(taskFileName, newText)
              file("task.html", newText)
            }
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }
  }

  @Test
  fun `test project with remote edu problems updated`() {
    val taskFileName = "Task.txt"
    val oldText = "old text"
    val oldCheckProfile = "old check profile"
    val newText = "new text"
    val newCheckProfile = "new check profile"
    val topic = "topic"

    localCourse = hyperskillCourseWithFiles(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(topic) {
          remoteEduTask(taskDescription = oldText, checkProfile = oldCheckProfile) {
            taskFile(taskFileName, oldText)
            taskFile("build.gradle")
          }
        }
      }
    }

    val tasks = localCourse.getTopicsSection()!!.getLesson(topic)!!.taskList
    updateCourseWithProblems(tasks.map {
      (it as RemoteEduTask).toTaskUpdate {
        descriptionText = newText
        checkProfile = newCheckProfile
        updateDate = Date(MINUTES.toMillis(2))
        getTaskFile(taskFileName)!!.text = newText
      }
    })

    runInEdtAndWait {
      fileTree {
        dir(HYPERSKILL_TOPICS) {
          dir("topic") {
            dir("task1") {
              file(taskFileName, newText)
              file("task.html", newText)
              file("build.gradle")
            }
          }
        }
        file("build.gradle")
        file("settings.gradle")
      }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
    }

    val task = localCourse.getTopicsSection()!!.getLesson(topic)!!.taskList[0] as RemoteEduTask
    assertEquals(newCheckProfile, task.checkProfile)
  }

  private fun <T: Task> T.toTaskUpdate(changeTask: T.() -> Unit): TaskUpdateInfo {
    val remoteTask = copy()
    remoteTask.changeTask()
    remoteTask.init(parent, false)
    return TaskUpdateInfo(this, remoteTask)
  }

  private fun updateCourseWithProblems(problemsUpdates: List<TaskUpdateInfo>,
                                       changeCourse: (HyperskillCourse.() -> Unit)? = null) {
    val remoteCourse = changeCourse?.let { toRemoteCourse(changeCourse) } ?: toRemoteCourse {}
    updateCourse(remoteCourse)
//    HyperskillCourseUpdater(project, localCourse).doUpdate(remoteCourse, problemsUpdates)
//    val isProjectUpToDate = remoteCourse == null || localCourse.getProjectLesson()?.shouldBeUpdated(project, remoteCourse) == false
//    assertTrue("Project is not up-to-date after update", isProjectUpToDate)
  }

  override fun createCourseWithFrameworkLessons() {
    localCourse = courseWithFiles(
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
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))
  }

  override fun getUpdater(localCourse: HyperskillCourse): CourseUpdater<HyperskillCourse> = HyperskillCourseUpdaterNew(project, localCourse)

  override fun initiateLocalCourse() {
    localCourse = createBasicHyperskillCourse()
  }
}
