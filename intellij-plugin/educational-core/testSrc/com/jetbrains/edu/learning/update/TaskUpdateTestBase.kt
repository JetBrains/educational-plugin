package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import kotlinx.coroutines.runBlocking
import java.util.*

abstract class TaskUpdateTestBase<T : Course> : UpdateTestBase<T>() {
  abstract fun getUpdater(lesson: Lesson): TaskUpdater

  protected fun updateTasks(remoteCourse: T, lesson: Lesson? = null, remoteLesson: Lesson? = null, isShouldBeUpdated: Boolean = true) {
    val lessonToBeUpdated = lesson ?: localCourse.lessons.first()
    val updater = getUpdater(lessonToBeUpdated)
    val lessonFromServer = remoteLesson ?: remoteCourse.lessons.first()
    val updates = runBlocking {
      updater.collect(lessonFromServer)
    }
    assertEquals("Updates are " + if (isShouldBeUpdated) "" else "not" + " available", isShouldBeUpdated, updates.isNotEmpty())
    val isUpdateSucceed = runBlocking {
      try {
        updater.update(lessonFromServer)
        true
      }
      catch (e: Exception) {
        thisLogger().error(e)
        false
      }
    }
    if (isShouldBeUpdated) {
      assertTrue("Update failed", isUpdateSucceed)
    }
  }

  fun `test nothing to update`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse { }
    updateTasks(remoteCourse, isShouldBeUpdated = false)

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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test task name has been updated`() {
    initiateLocalCourse()

    val newTaskName = "taskNewName"
    val remoteCourse = toRemoteCourse {
      taskList[0].name = newTaskName
    }
    updateTasks(remoteCourse)

    val taskName = findTask(0, 0).name
    assertEquals("Task name not updated", newTaskName, taskName)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test task description has been updated`() {
    initiateLocalCourse()

    val newDescription = "new description"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        descriptionText = newDescription
        updateDate = Date(100)
      }
    }
    updateTasks(remoteCourse)

    val taskDescription = runReadAction {
      findTask(0, 0).getTaskTextFromTask(project)!!
    }
    assertTrue("Task Description not updated", taskDescription.contains(newDescription))

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
          file("task.html", newDescription)
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test task type has been updated`() {
    initiateLocalCourse()
    val newTaskName = "taskNewName"
    val newCodeTask = CodeTask(newTaskName).apply {
      id = 1
      descriptionFormat = DescriptionFormat.HTML
      taskFiles = linkedMapOf(
        "src/Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "test/Tests1.kt" to TaskFile("test/Tests1.kt", "fun test1() {}")
      )
    }
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        removeTask(taskList[0])
        addTask(0, newCodeTask)
      }
    }
    updateTasks(remoteCourse)

    assertTrue("Task type isn't changed", findTask(0, 0) is CodeTask)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
          dir("src") {
            file("Task.kt")
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test taskFile name has been updated`() {
    initiateLocalCourse()
    val newTaskFile = TaskFile("src/TaskFile2Renamed.kt", "task file 2 text")
    val newTask = EduTask("task2").apply {
      id = 2
      descriptionFormat = DescriptionFormat.HTML
      taskFiles = localCourse.taskList[1].taskFiles
      removeTaskFile("src/Task.kt")
      addTaskFile(newTaskFile)
    }
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        removeTask(taskList[1])
        addTask(1, newTask)
      }
    }
    updateTasks(remoteCourse)

    assertTrue("taskFile name isn't changed", findTask(0, 1).taskFiles[newTaskFile.name] != null)

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
            file("TaskFile2Renamed.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test taskFiles have been updated`() {
    initiateLocalCourse()
    val newTaskFile = TaskFile("src/newTaskFile.kt", "New task file text")
    val newTask = EduTask("task2").apply {
      id = 2
      descriptionFormat = DescriptionFormat.HTML
      taskFiles = localCourse.taskList[1].taskFiles
      addTaskFile(newTaskFile)
    }
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        removeTask(taskList[1])
        addTask(1, newTask)
      }
    }
    updateTasks(remoteCourse)

    assertTrue("New TaskFile isn't added", findTask(0, 1).taskFiles[newTaskFile.name] != null)

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
            file("newTaskFile.kt")
          }
          dir("test") {
            file("Tests2.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test save task status Solved if task not updated`() {
    initiateLocalCourse()
    localCourse.taskList[0].status = CheckStatus.Solved
    val remoteCourse = toRemoteCourse {
      taskList[0].status = CheckStatus.Unchecked
    }
    updateTasks(remoteCourse, isShouldBeUpdated = false)

    assertEquals("Solved task status has been updated", CheckStatus.Solved, findTask(0, 0).status)

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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test save task status Solved if task was updated`() {
    initiateLocalCourse()
    val newTaskName = "taskNewName"
    localCourse.taskList[0].status = CheckStatus.Solved
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        name = newTaskName
        status = CheckStatus.Unchecked
      }
    }
    updateTasks(remoteCourse)

    assertEquals("Solved task status has been updated", CheckStatus.Solved, findTask(0, 0).status)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test save task status Failed if task not updated`() {
    initiateLocalCourse()
    localCourse.taskList[0].status = CheckStatus.Failed
    val remoteCourse = toRemoteCourse {
      taskList[0].status = CheckStatus.Unchecked
    }
    updateTasks(remoteCourse, isShouldBeUpdated = false)

    assertEquals("Failed task status has been updated", CheckStatus.Failed, findTask(0, 0).status)

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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  fun `test do not save task status Failed if task was updated`() {
    initiateLocalCourse()
    val newTaskName = "taskNewName"
    localCourse.taskList[0].status = CheckStatus.Failed
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        name = newTaskName
        status = CheckStatus.Unchecked
      }
    }
    updateTasks(remoteCourse)

    assertEquals("Failed task status hasn't been updated", CheckStatus.Unchecked, findTask(0, 0).status)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  protected val T.taskList: List<Task> get() = lessons[0].taskList
}