package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.ide.actions.DeleteAction
import com.intellij.openapi.ui.TestDialog
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCActionTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.withTestDialog

class CCDeleteActionTest : CCActionTestCase() {

  fun `test delete task`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1")
        eduTask("task2")
      }
    }

    val taskFile = findFile("lesson1/task1")

    val testDialog = TestDeleteDialog()
    withTestDialog(testDialog) {
      testAction(dataContext(taskFile), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("lesson1") {
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test delete task with dependent tasks`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
        eduTask("task2") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "lesson1#task1#Task.kt#1")
          }
        }
        eduTask("task3") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "lesson1#task2#Task.kt#1")
          }
        }
      }
    }

    val task2 = course.getLesson("lesson1")!!.getTask("task2")!!
    val task3 = course.getLesson("lesson1")!!.getTask("task3")!!
    val testDialog = TestDeleteDialog(listOf(task2), listOf(task3))

    val taskFile = findFile("lesson1/task1")
    withTestDialog(testDialog) {
      testAction(dataContext(taskFile), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("lesson1") {
        dir("task2") {
          file("Task.kt")
          file("task.html")
        }
        dir("task3") {
          file("Task.kt")
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertNull(task2.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task3.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
  }

  fun `test delete lesson`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1")
      }
      lesson {
        eduTask("task2")
      }
    }

    val lessonFile = findFile("lesson1")
    withTestDialog(TestDialog.OK) {
      testAction(dataContext(lessonFile), DeleteAction())
    }

    fileTree {
      dir("lesson2") {
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test lesson deletion with dependent tasks`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
        eduTask("task2") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "lesson1#task1#Task.kt#1")
          }
        }
      }
      lesson("lesson2") {
        eduTask("task3") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "lesson1#task2#Task.kt#1")
          }
        }
        eduTask("task4") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "lesson2#task3#Task.kt#1")
          }
        }
      }
    }

    val task2 = course.getLesson("lesson1")!!.getTask("task2")!!
    val task3 = course.getLesson("lesson2")!!.getTask("task3")!!
    val task4 = course.getLesson("lesson2")!!.getTask("task4")!!
    val testDialog = TestDeleteDialog(listOf(task3), listOf(task2, task4))

    val lessonFile = findFile("lesson1")
    withTestDialog(testDialog) {
      testAction(dataContext(lessonFile), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("lesson2") {
        dir("task3") {
          file("Task.kt")
          file("task.html")
        }
        dir("task4") {
          file("Task.kt")
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertNull(task3.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task4.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
  }

  fun `test delete section`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
      lesson("lesson2") {
        eduTask("task2")
      }
    }

    val sectionFile = findFile("section1")
    
    val testDialog = TestDeleteDialog()
    withTestDialog(testDialog) {
      testAction(dataContext(sectionFile), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("lesson2") {
        dir("task2") {
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test section deletion with dependent tasks`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo")
            }
          }
          eduTask("task2") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo", "section1#lesson1#task1#Task.kt#1")
            }
          }
        }
      }
      section("section2") {
        lesson("lesson2") {
          eduTask("task3") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo", "section1#lesson1#task2#Task.kt#1")
            }
          }
          eduTask("task4") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo", "section2#lesson2#task3#Task.kt#1")
            }
          }
        }
      }
      lesson("lesson3") {
        eduTask("task5") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "section1#lesson1#task2#Task.kt#1")
          }
        }
        eduTask("task6") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", "section2#lesson2#task3#Task.kt#1")
          }
        }
      }
    }

    val task3 = course.getLesson("section2", "lesson2")!!.getTask("task3")!!
    val task4 = course.getLesson("section2", "lesson2")!!.getTask("task4")!!
    val task5 = course.getLesson("lesson3")!!.getTask("task5")!!
    val task6 = course.getLesson("lesson3")!!.getTask("task6")!!
    val testDialog = TestDeleteDialog(listOf(task3, task5), listOf(task4, task6))

    val section1 = findFile("section1")
    withTestDialog(testDialog) {
      testAction(dataContext(section1), DeleteAction())
    }.checkWasShown()

    fileTree {
      dir("section2") {
        dir("lesson2") {
          dir("task3") {
            file("Task.kt")
            file("task.html")
          }
          dir("task4") {
            file("Task.kt")
            file("task.html")
          }
        }
      }
      dir("lesson3") {
        dir("task5") {
          file("Task.kt")
          file("task.html")
        }
        dir("task6") {
          file("Task.kt")
          file("task.html")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertNull(task3.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNull(task5.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task4.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task6.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
  }

  private class TestDeleteDialog(
    private val expectedTasks: List<Task> = emptyList(),
    private val notExpectedTasks: List<Task> = emptyList()
  ) : EduTestDialog() {

    override fun show(message: String): Int {
      val result = super.show(message)
      for (task in expectedTasks) {
        val taskMessageName = CCStudyItemDeleteProvider.taskMessageName(task)
        if (taskMessageName !in message) {
          error("Can't find `$taskMessageName` in dialog message: `$message`")
        }
      }
      for (task in notExpectedTasks) {
        val taskMessageName = CCStudyItemDeleteProvider.taskMessageName(task)
        if (taskMessageName in message) {
          error("`$taskMessageName` isn't expected in dialog message: `$message`")
        }
      }
      return result
    }
  }
}
