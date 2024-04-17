package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.application.runReadAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.TaskUpdateTestBase
import com.jetbrains.edu.learning.update.TaskUpdater
import org.junit.Test
import java.util.*

class MarketplaceTaskUpdateTest : TaskUpdateTestBase<EduCourse>() {

  override fun getUpdater(lesson: Lesson): TaskUpdater = MarketplaceTaskUpdater(project, lesson)

  @Test
  fun `test task description with placeholders have been updated`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() { <p>TODO</p>() }") {
            placeholder(index = 0, placeholderText = "TODO")
          }
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
    } as EduCourse

    val newText = "TODO()"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        descriptionText = "fun foo() { <p>$newText</p> }"
        updateDate = Date(100)
        taskFiles["src/Task.kt"]?.answerPlaceholders?.first()?.placeholderText = newText
      }
    }
    updateTasks(remoteCourse)

    val taskDescription = runReadAction {
      findTask(0, 0).getTaskTextFromTask(project)!!
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
        "Tests1.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        addTask(newEduTask)
      }
    }
    updateTasks(remoteCourse)

    assertEquals("Task hasn't been added", 3, findLesson(0).taskList.size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
          }
          file("task.html")
        }
        dir("task3") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests3.kt")
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
      lessons.first().apply {
        removeTask(taskList[1])
      }
    }
    updateTasks(remoteCourse)

    assertEquals("Task hasn't been deleted", 1, findLesson(0).taskList.size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests1.kt")
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
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as EduCourse
    localCourse.marketplaceCourseVersion = 1
  }
}