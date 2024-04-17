package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.junit.Test

class CCDeleteActionTest : EduActionTestCase() {
  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().messageBus
      .connect(testRootDisposable)
      .subscribe(VirtualFileManager.VFS_CHANGES, CCVirtualFileListener(project, testRootDisposable))
  }

  @Test
  fun `test delete task`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask("task1")
        eduTask("task2")
      }
    }

    val taskFile = findFile("lesson1/task1")

    val testDialog = TestDeleteDialog()
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(taskFile))
    }.checkWasShown()

    fileTree {
      dir("lesson1") {
        dir("task2") {
          file("task.md")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  @Test
  fun `test delete task with dependent tasks`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
        eduTask("task2") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "lesson1#task1#Task.kt#1")
          }
        }
        eduTask("task3") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "lesson1#task2#Task.kt#1")
          }
        }
      }
    }

    val task2 = course.getLesson("lesson1")!!.getTask("task2")!!
    val task3 = course.getLesson("lesson1")!!.getTask("task3")!!
    val testDialog = TestDeleteDialog(listOf(task2), listOf(task3))

    val taskFile = findFile("lesson1/task1")
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(taskFile))
    }.checkWasShown()

    fileTree {
      dir("lesson1") {
        dir("task2") {
          file("Task.kt")
          file("task.md")
        }
        dir("task3") {
          file("Task.kt")
          file("task.md")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertNull(task2.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task3.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
  }

  @Test
  fun `test delete lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask("task1")
      }
      lesson {
        eduTask("task2")
      }
    }

    val lessonFile = findFile("lesson1")
    withEduTestDialog(EduTestDialog()) {
      testAction(IdeActions.ACTION_DELETE, dataContext(lessonFile))
    }

    fileTree {
      dir("lesson2") {
        dir("task2") {
          file("task.md")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertEquals(1, course.items.size)
    assertNull(course.getLesson("lesson1"))
    assertEquals(1, course.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test lesson deletion with dependent tasks`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo")
          }
        }
        eduTask("task2") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
      lesson("lesson2") {
        eduTask("task3") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "lesson1#task2#Task.kt#1")
          }
        }
        eduTask("task4") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "lesson2#task3#Task.kt#1")
          }
        }
      }
    }

    val task2 = course.getLesson("lesson1")!!.getTask("task2")!!
    val task3 = course.getLesson("lesson2")!!.getTask("task3")!!
    val task4 = course.getLesson("lesson2")!!.getTask("task4")!!
    val testDialog = TestDeleteDialog(listOf(task3), listOf(task2, task4))

    val lessonFile = findFile("lesson1")
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(lessonFile))
    }.checkWasShown()

    fileTree {
      dir("lesson2") {
        dir("task3") {
          file("Task.kt")
          file("task.md")
        }
        dir("task4") {
          file("Task.kt")
          file("task.md")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertNull(task3.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task4.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
  }

  @Test
  fun `test delete section`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(sectionFile))
    }.checkWasShown()

    fileTree {
      dir("lesson2") {
        dir("task2") {
          file("task.md")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  @Test
  fun `test delete middle section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section()
      lesson()
    }
    val sectionFile = findFile("section2")
    val testDialog = TestDeleteDialog()
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(sectionFile))
    }.checkWasShown()

    assertEquals(2, course.items.size)
    assertNull(course.getSection("section2"))
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test delete lesson from section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson()
        lesson()
      }
      lesson()
    }
    val lesson1 = findFile("section2/lesson1")
    val testDialog = TestDeleteDialog()
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(lesson1))
    }.checkWasShown()

    val section = course.getSection("section2")!!
    assertEquals(3, course.items.size)
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, section.index)
    assertEquals(3, course.getLesson("lesson2")!!.index)

    assertNull(section.getLesson("lesson1"))
    assertEquals(1, section.items.size)
    assertEquals(1, section.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test delete not empty section`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson()
      }
      lesson()
    }

    val sectionFile = findFile("section2")
    val testDialog = TestDeleteDialog()
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(sectionFile))
    }.checkWasShown()

    assertEquals(2, course.items.size)
    assertNull(course.getSection("section2"))
    assertEquals(1, course.getLesson("lesson1")!!.index)
    assertEquals(2, course.getLesson("lesson2")!!.index)
  }

  @Test
  fun `test section deletion with dependent tasks`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo")
            }
          }
          eduTask("task2") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo", dependency = "section1#lesson1#task1#Task.kt#1")
            }
          }
        }
      }
      section("section2") {
        lesson("lesson2") {
          eduTask("task3") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo", dependency = "section1#lesson1#task2#Task.kt#1")
            }
          }
          eduTask("task4") {
            taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "Foo", dependency = "section2#lesson2#task3#Task.kt#1")
            }
          }
        }
      }
      lesson("lesson3") {
        eduTask("task5") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "section1#lesson1#task2#Task.kt#1")
          }
        }
        eduTask("task6") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "Foo", dependency = "section2#lesson2#task3#Task.kt#1")
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
    withEduTestDialog(testDialog) {
      testAction(IdeActions.ACTION_DELETE, dataContext(section1))
    }.checkWasShown()

    fileTree {
      dir("section2") {
        dir("lesson2") {
          dir("task3") {
            file("Task.kt")
            file("task.md")
          }
          dir("task4") {
            file("Task.kt")
            file("task.md")
          }
        }
      }
      dir("lesson3") {
        dir("task5") {
          file("Task.kt")
          file("task.md")
        }
        dir("task6") {
          file("Task.kt")
          file("task.md")
        }
      }
    }.assertEquals(LightPlatformTestCase.getSourceRoot())

    assertNull(task3.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNull(task5.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task4.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
    assertNotNull(task6.getTaskFile("Task.kt")!!.answerPlaceholders[0].placeholderDependency)
  }

  class TestDeleteDialog(
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
