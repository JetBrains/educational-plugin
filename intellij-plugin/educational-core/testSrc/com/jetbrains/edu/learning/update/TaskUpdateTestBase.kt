package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.runReadAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskText
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import org.junit.Test
import java.util.*

abstract class TaskUpdateTestBase<T : Course> : UpdateTestBase<T>() {

  @Test
  fun `test task name has been updated`() {
    initiateLocalCourse()

    val newTaskName = "taskNewName"
    val remoteCourse = toRemoteCourse {
      taskList[0].name = newTaskName
    }

    updateCourse(remoteCourse)

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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test task description has been updated`() {
    initiateLocalCourse()

    val newDescription = "new description"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        descriptionText = newDescription
        updateDate = Date(100)
      }
    }

    updateCourse(remoteCourse)

    val taskDescription = runReadAction {
      findTask(0, 0).getTaskText(project)!!
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
            file("Tests.kt")
          }
          file("task.html", newDescription)
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test task type has been updated`() {
    initiateLocalCourse()

    val newTaskName = "taskNewName"
    val newCodeTask = CodeTask(newTaskName).apply {
      id = 1
      descriptionFormat = DescriptionFormat.HTML
      taskFiles = linkedMapOf(
        "src/Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "test/Tests.kt" to TaskFile("test/Tests.kt", "fun test1() {}")
      )
    }
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        removeTask(taskList[0])
        addTask(0, newCodeTask)
      }
    }

    updateCourse(remoteCourse)

    assertTrue("Task type isn't changed", findTask(0, 0) is CodeTask)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
          dir("src") {
            file("Task.kt")
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test taskFile name has been updated`() {
    initiateLocalCourse()

    val newTaskFile = TaskFile("src/TaskFile2Renamed.kt", "task file 2 text")
    val remoteCourse = toRemoteCourse {
      lessons.first().taskList[1].apply {
        removeTaskFile("src/Task.kt")
        addTaskFile(newTaskFile)
      }
    }
    updateCourse(remoteCourse)

    assertEquals("taskFile name isn't changed", newTaskFile.name, localCourse.lessons[0].taskList[1].taskFiles[newTaskFile.name]!!.name)

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
            file("TaskFile2Renamed.kt")
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
  fun `test taskFiles have been added`() {
    initiateLocalCourse()

    val newTaskFile = TaskFile("src/newTaskFile.kt", "New task file text")
    val remoteCourse = toRemoteCourse {
      lessons[0].taskList[1].addTaskFile(newTaskFile)
    }

    updateCourse(remoteCourse)

    assertEquals("New TaskFile isn't added", newTaskFile, localCourse.lessons[0].taskList[1].taskFiles[newTaskFile.name])

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
            file("newTaskFile.kt")
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
  fun `test save task status Solved if task not updated`() {
    initiateLocalCourse()
    localCourse.taskList[0].status = CheckStatus.Solved

    val remoteCourse = toRemoteCourse {
      taskList[0].status = CheckStatus.Unchecked
    }

    updateCourse(remoteCourse, isShouldBeUpdated = false)

    assertEquals("Solved task status has been updated", CheckStatus.Solved, localCourse.taskList[0].status)

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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test save task status Solved if task was updated`() {
    initiateLocalCourse()
    localCourse.taskList[0].status = CheckStatus.Solved

    val newTaskName = "taskNewName"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        name = newTaskName
        status = CheckStatus.Unchecked
      }
    }
    updateCourse(remoteCourse)

    assertEquals("Solved task status has been updated", CheckStatus.Solved, localCourse.taskList[0].status)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test save task status Failed if task not updated`() {
    initiateLocalCourse()
    localCourse.taskList[0].status = CheckStatus.Failed

    val remoteCourse = toRemoteCourse {
      taskList[0].status = CheckStatus.Unchecked
    }

    updateCourse(remoteCourse, isShouldBeUpdated = false)

    assertEquals("Failed task status has been updated", CheckStatus.Failed, localCourse.taskList[0].status)

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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test do not save task status Failed if task was updated`() {
    initiateLocalCourse()
    localCourse.taskList[0].status = CheckStatus.Failed

    val newTaskName = "taskNewName"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        name = newTaskName
        status = CheckStatus.Unchecked
      }
    }

    updateCourse(remoteCourse)

    assertEquals("Failed task status hasn't been updated", CheckStatus.Unchecked, localCourse.taskList[0].status)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir(newTaskName) {
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
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  protected val T.taskList: List<Task> get() = lessons[0].taskList
}