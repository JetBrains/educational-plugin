package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.application.runReadAction
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskText
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.CourseUpdater
import com.jetbrains.edu.learning.update.TaskUpdateTestBase
import org.junit.Test
import java.util.*

class MarketplaceTaskUpdateTest : TaskUpdateTestBase<EduCourse>() {
  override fun getUpdater(localCourse: EduCourse): CourseUpdater<EduCourse> = MarketplaceCourseUpdaterNew(project, localCourse)

  @Test
  fun `test task description with placeholders have been updated`() {
    localCourse = createBasicMarketplaceCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description") {
          taskFile("src/Task.kt", "fun foo() { <p>TODO</p>() }") {
            placeholder(index = 0, placeholderText = "TODO")
          }
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun test2() {}")
        }
      }
    }

    val newText = "TODO()"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        descriptionText = "fun foo() { <p>$newText</p> }"
        updateDate = Date(100)
        taskFiles["src/Task.kt"]!!.answerPlaceholders[0].placeholderText = newText
      }
    }

    updateCourse(remoteCourse)

    val taskDescription = runReadAction {
      findTask(0, 0).getTaskText(project)!!
    }
    assertTrue("Task Description not updated", taskDescription.contains(newText))
  }

  @Test
  fun `test new task created`() {
    initiateLocalCourse()

    val newEduTask = EduTask("task3").apply {
      id = 3
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val remoteCourse = toRemoteCourse {
      lessons[0].apply {
        addTask(newEduTask)
      }
    }

    updateCourse(remoteCourse)

    assertEquals("Task hasn't been added", 3, localCourse.lessons[0].taskList.size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task3") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new task created in the middle of the lesson`() {
    localCourse = createBasicMarketplaceCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1)
        eduTask("task2", stepId = 2)
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }

    val newEduTask = EduTask("task3").apply { id = 3; index = 2 }
    val remoteCourse = toRemoteCourse {
      lessons[0].apply {
        taskList[1].index = 3
        addTask(newEduTask)
        sortItems()
      }
    }
    updateCourse(remoteCourse)

    val tasks = localCourse.lessons[0].taskList
    assertEquals("Task hasn't been added", 3, tasks.size)
    checkIndices(tasks)
    tasks[0].let { task ->
      assertEquals(1, task.id)
      assertEquals(1, task.index)
      assertEquals("task1", task.name)
      assertEquals("task1", task.presentableName)
    }
    tasks[1].let { task ->
      assertEquals(3, task.id)
      assertEquals(2, task.index)
      assertEquals("task3", task.name)
      assertEquals("task3", task.presentableName)
    }
    tasks[2].let { task ->
      assertEquals(2, task.id)
      assertEquals(3, task.index)
      assertEquals("task2", task.name)
      assertEquals("task2", task.presentableName)
    }

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test first task deleted`() {
    initiateLocalCourse()
    val remoteCourse = toRemoteCourse {
      lessons[0].removeTask(lessons[0].taskList[0])
    }
    updateCourse(remoteCourse)

    assertEquals("Task hasn't been deleted", 1, findLesson(0).taskList.size)
    assertEquals("Task index hasn't been changed", 1, findLesson(0).taskList[0].index)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test last task deleted`() {
    initiateLocalCourse()
    val remoteCourse = toRemoteCourse {
      lessons[0].removeTask(taskList[1])
    }
    updateCourse(remoteCourse)

    assertEquals("Task hasn't been deleted", 1, localCourse.lessons[0].taskList.size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    localCourse = createBasicMarketplaceCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun test2() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }
  }
}