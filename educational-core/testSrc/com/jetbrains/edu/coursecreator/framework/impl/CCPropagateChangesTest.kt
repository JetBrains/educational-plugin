package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vcs.merge.MergeSession.Resolution
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.CCApplyChangesToNextTasks
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.coursecreator.framework.diff.withFLMultipleFileMergeUI
import kotlin.test.assertFails

class CCPropagateChangesTest : EduActionTestCase() {
  fun `test changes in files propagate until cancel`() {
    val course = createFrameworkCourse(3)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      val task3 = course.findTask("lesson1", "task3")
      task3.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun baz() {}\n")
      task.openTaskFileInEditor("src/Task.kt")
      doTest(task, listOf(Resolution.AcceptedYours, Resolution.AcceptedYours), 1)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task3") {
          dir("src") {
            file("Task.kt", """
              fun baz() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test propagate new file`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      doTest(task, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
            file("Bar.kt", "fun bar() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
            file("Bar.kt", "fun bar() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test propagate remove file`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      runWriteAction {
        findFile("lesson1/task1/src/Baz.kt").delete(CCPropagateChangesTest::class.java)
      }
      doTest(task, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test invoke action from lesson node`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      doTest(task.parent, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test cannot invoke action from course node`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      assertFails {
        doTest(task.course, listOf(Resolution.AcceptedYours))
      }
    }
  }

  fun `test invoke action from second task`() {
    val course = createFrameworkCourse(3)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task2")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      doTest(task, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task3") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test changes in invisible files do not propagate`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("test/Tests.kt")
      myFixture.type("fun bar() {}\n")
      doTest(task, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", """
              fun bar() {}
              fun tests() {}
            """)
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test files remain the same after propagation if changes haven't been made in previous tasks`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      runWriteAction {
        findFile("lesson1/task2/src/Baz.kt").delete(CCPropagateChangesTest::class.java)
      }
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task2/src/Bar.kt", "fun bar() {}")
      val task1 = course.findTask("lesson1", "task1")
      doTest(task1, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", "fun foo() {}")
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Bar.kt", "fun bar() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test new files do change if not resolved during merge`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun f() {}\n")

      doTest(task1, listOf(Resolution.AcceptedYours))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  fun `test new files are added or deleted or changed if resolved during merge`() {
    val course = createFrameworkCourse(2)

    withVirtualFileListener(course) {
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun f() {}\n")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task2/src/Bar.kt", "fun bar() {}")
      runWriteAction {
        findFile("lesson1/task2/src/Baz.kt").delete(CCPropagateChangesTest::class.java)
      }

      doTest(task1, listOf(Resolution.AcceptedTheirs))
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt", """
              fun bar() {}
              fun foo() {}
            """)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt", """
              fun f() {}
              fun foo() {}
            """)
            file("Bar.kt", "fun bar() {}")
          }
          dir("test") {
            file("Tests.kt", "fun tests() {}")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  private fun createFrameworkCourse(numberOfTasks: Int): Course = courseWithFiles(
    courseMode = CourseMode.EDUCATOR,
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1") {
      repeat(numberOfTasks) {
        eduTask("task${it + 1}") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun tests() {}")
        }
      }
    }
  }.apply {
    val task = course.findTask("lesson1", "task1")
    doTest(task, List(numberOfTasks - 1) { Resolution.AcceptedYours })
  }

  private fun doTest(
    item: StudyItem,
    resolutions: List<Resolution>,
    // cancel on conflict resolution with number [cancelOnConflict]
    cancelOnConflict: Int = Int.MAX_VALUE,
  ) {
    val mockUI = MockFLMultipleFileMergeUI(resolutions, cancelOnConflict)
    withFLMultipleFileMergeUI(mockUI) {
      val dataContext = dataContext(item.getDir(project.courseDir)!!)
      testAction(CCApplyChangesToNextTasks.ACTION_ID, dataContext)
    }
  }

  private val rootDir: VirtualFile
    get() = LightPlatformTestCase.getSourceRoot()
}