package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.ui.SelectTaskUi
import com.jetbrains.edu.coursecreator.ui.withMockSelectTaskUi
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree

class CCSolveAllTasksBeforeActionTest : EduActionTestCase() {

  fun `test solve all tasks before action`() {
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Foo.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
          taskFile("Bar.kt", "fn bar(<p>params</p>) = <p>TODO()</p>") {
            placeholder(0, "a: Int")
            placeholder(1, "a + 10")
          }
        }
        eduTask("task2") {
          taskFile("Baz.kt", "fn baz() = <p>TODO()</p>") {
            placeholder(0, "true")
          }
        }
      }
      lesson("lesson2") {
        eduTask("task3") {
          taskFile("Foo.kt", "fn foo() = <p>TODO()</p> + <p>TODO()</p>") {
            placeholder(0, "456")
            placeholder(1, "789")
          }
        }
        eduTask("task4") {
          taskFile("Qqq.kt", "fn qqq() = <p>TODO()</p>") {
            placeholder(0, "1.23")
          }
        }
      }
    }

    doTest("lesson2", "task4") {
      dir("lesson1") {
        dir("task1") {
          file("Foo.kt", "fn foo() = 123")
          file("Bar.kt", "fn bar(a: Int) = a + 10")
          file("task.html")
        }
        dir("task2") {
          file("Baz.kt", "fn baz() = true")
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task3") {
          file("Foo.kt", "fn foo() = 123 + 789")
          file("task.html")
        }
        dir("task4") {
          file("Qqq.kt", "fn qqq() = TODO()")
          file("task.html")
        }
      }
    }
  }

  fun `test solve all tasks before action with framework lessons`() {
    courseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("Foo.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }

        eduTask("task2") {
          taskFile("Foo.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
          taskFile("Bar.kt", "fn bar(<p>params</p>) = <p>TODO()</p>") {
            placeholder(0, "a: Int")
            placeholder(1, "a + 10")
          }
        }

        eduTask("task3") {
          taskFile("Foo.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
          taskFile("Bar.kt", "fn bar(<p>params</p>) = <p>TODO()</p>") {
            placeholder(0, "b: Int")
            placeholder(1, "b + 10")
          }
          taskFile("Baz.kt", "fn baz() = <p>TODO()</p> + <p>TODO()</p>") {
            placeholder(0, "456")
            placeholder(1, "789")
          }
        }
      }
    }

    doTest("lesson1", "task3") {
      dir("lesson1") {
        dir("task") {
          file("Foo.kt", "fn foo() = 123")
          file("Bar.kt", "fn bar(a: Int) = a + 10")
          file("Baz.kt", "fn baz() = 123 + TODO()")
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
    }
  }

  private fun doTest(lessonName: String, taskName: String, expectedFileTree: FileTreeBuilder.() -> Unit) {
    withMockSelectTaskUi(object : SelectTaskUi {
      override fun selectTask(project: Project, course: EduCourse): Task {
        return course.findTask(lessonName, taskName)
      }
    }) {
      val registryValue = Registry.get(CCSolveAllTasksBeforeAction.REGISTRY_KEY)
      val oldValue = registryValue.asBoolean()
      registryValue.setValue(true)
      try {
        myFixture.testAction(CCSolveAllTasksBeforeAction())
      } finally {
        registryValue.setValue(oldValue)
      }
    }

    fileTree(expectedFileTree).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }
}
