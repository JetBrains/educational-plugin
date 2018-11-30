package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import org.junit.Assert.assertArrayEquals

class CCCreateFrameworkTaskTest : EduActionTestCase() {

  private val root: VirtualFile get() = LightPlatformTestCase.getSourceRoot()

  override fun getTestDataPath(): String = super.getTestDataPath() + "/actions/frameworkLessons"

  fun `test first task in framework lesson`() {
    val lessonName = "FrameworkLesson"
    val taskName = "Task"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson(lessonName)
    }

    val lessonFile = findFile(lessonName)

    withMockCreateStudyItemUi(MockNewStudyItemUi(taskName)) {
      withVirtualFileListener(course) {
        testAction(dataContext(lessonFile), CCCreateTask())
      }
    }

    fileTree {
      dir(lessonName) {
        dir(taskName) {
          file("Task.txt")
          file("Tests.txt")
          file("task.html")
        }
      }
    }.assertEquals(root)

    assertEquals(1, course.lessons[0].taskList.size)
  }

  fun `test new task in framework lesson`() {
    val lessonName = "FrameworkLesson"

    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson(lessonName) {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
          taskFile("build.gradle")
        }
      }
    }

    val newTaskName = "task2"
    val lessonFile = findFile(lessonName)

    withMockCreateStudyItemUi(MockNewStudyItemUi(newTaskName)) {
      withVirtualFileListener(course) {
        testAction(dataContext(lessonFile), CCCreateTask())
      }
    }

    fileTree {
      dir(lessonName) {
        dir("task1") {
          file("Task.kt", "fun foo(): String = \"Foo\"")
          file("build.gradle")
          file("task.html")
        }
        dir(newTaskName) {
          file("Task.kt", "fun foo(): String = \"Foo\"")
          file("build.gradle")
          file("task.html")
        }
      }
    }.assertEquals(root, myFixture)

    assertEquals(2, course.lessons[0].taskList.size)
    val createdTask = course.lessons[0].taskList[1]
    assertEquals(2, createdTask.taskFiles.size)
    val prevTaskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"] ?: error("Can't find `Task.kt` file")
    val taskFile = createdTask.taskFiles["Task.kt"] ?: error("Can't find `Task.kt` file")
    assertEquals(prevTaskFile.name, taskFile.name)
    assertEquals(1, taskFile.answerPlaceholders.size)
    val placeholder = taskFile.answerPlaceholders[0]
    val targetPlaceholder = placeholder.placeholderDependency?.resolve(course) ?: error("Can't resolve placeholder dependency")
    assertEquals(prevTaskFile.answerPlaceholders[0], targetPlaceholder)
  }

  fun `test new task in the middle of lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson {
        eduTask {
          taskFile("Task.kt", """
            fun foo(): String = <p>TODO()</p>
            fun bar(): String = <p>TODO()</p>
          """) {
            placeholder(0, "\"Foo\"")
            placeholder(1, "\"Bar\"")
          }
        }
        eduTask {
          taskFile("Foo.kt", """
            fun foo(): String = <p>TODO()</p>
          """) {
            placeholder(0, "\"Foo\"", "lesson1#task1#Task.kt#1")
          }
          taskFile("Bar.kt", """
            fun bar(): String = <p>TODO()</p>
          """) {
            placeholder(0, "\"Bar\"", "lesson1#task1#Task.kt#2")
          }
        }
      }
    }
    val task1 = course.lessons[0].getTask("task1") ?: error("Can't find `task1`")
    val task2 = course.lessons[0].getTask("task2") ?: error("Can't find `task2`")

    val firstTaskFile = findFile("lesson1/task1")
    withMockCreateStudyItemUi(MockNewStudyItemUi("task1.5", 2)) {
      testAction(dataContext(firstTaskFile), CCCreateTask())
    }

    val insertedTask = course.lessons[0].getTask("task1.5") ?: error("Can't find `task1.5`")
    val taskFile = insertedTask.getTaskFile("Task.kt") ?: error("Can't find `Task.kt` in `task1.5`")

    // Check that all placeholders of new task refer to prev task
    assertEquals(task1.getTaskFile("Task.kt")!!.answerPlaceholders[0], taskFile.answerPlaceholders[0].placeholderDependency?.resolve(course))
    assertEquals(task1.getTaskFile("Task.kt")!!.answerPlaceholders[1], taskFile.answerPlaceholders[1].placeholderDependency?.resolve(course))

    // Check that all placeholders of next task refer to new task
    assertEquals(taskFile.answerPlaceholders[0], task2.getTaskFile("Foo.kt")?.answerPlaceholders?.get(0)?.placeholderDependency?.resolve(course))
    assertEquals(taskFile.answerPlaceholders[1], task2.getTaskFile("Bar.kt")?.answerPlaceholders?.get(0)?.placeholderDependency?.resolve(course))
  }

  fun `test copy images`() {
    val lessonName = "lesson1"
    val imageName = "image.png"

    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson(lessonName) {
        eduTask {
          taskFile("Task.kt")
          taskFileFromResources(imageName, "$testDataPath/$imageName")
        }
      }
    }
    val newTaskName = "task2"
    val lessonFile = findFile(lessonName)

    withVirtualFileListener(course) {
      withMockCreateStudyItemUi(MockNewStudyItemUi(newTaskName)) {
        testAction(dataContext(lessonFile), CCCreateTask())
      }
    }

    fileTree {
      dir(lessonName) {
        dir("task1") {
          file("Task.kt")
          file(imageName)
          file("task.html")
        }
        dir(newTaskName) {
          file("Task.kt")
          file(imageName)
          file("task.html")
        }
      }
    }.assertEquals(root)

    val originalImage = findFile("$lessonName/task1/$imageName")
    val imageCopy = findFile("$lessonName/$newTaskName/$imageName")

    assertArrayEquals("Contents of `$originalImage` and `$imageCopy` differ",
                      VfsUtil.loadBytes(originalImage),
                      VfsUtil.loadBytes(imageCopy))
  }

  fun `test copy actual text of files`() {
    val lessonName = "lesson1"
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson(lessonName) {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
          taskFile("build.gradle", "apply plugin: \"kotlin\"")
        }
      }
    }

    typeAtTheEndOfFile("lesson1/task1/Task.kt", "\nfun bar(): String = TODO()")
    typeAtTheEndOfFile("lesson1/task1/build.gradle", "\napply plugin: \"java\"")

    val newTaskName = "task2"
    val lessonFile = findFile(lessonName)

    withVirtualFileListener(course) {
      withMockCreateStudyItemUi(MockNewStudyItemUi(newTaskName)) {
        testAction(dataContext(lessonFile), CCCreateTask())
      }
    }

    fileTree {
      dir(lessonName) {
        dir("task1") {
          file("Task.kt", """
            fun foo(): String = TODO()
            fun bar(): String = TODO()
          """)
          file("build.gradle", """
            apply plugin: "kotlin"
            apply plugin: "java"
          """)
          file("task.html")
        }
        dir(newTaskName) {
          file("Task.kt", """
            fun foo(): String = TODO()
            fun bar(): String = TODO()
          """)
          file("build.gradle", """
            apply plugin: "kotlin"
            apply plugin: "java"
          """)
          file("task.html")
        }
      }
    }.assertEquals(root, myFixture)
  }

  private fun typeAtTheEndOfFile(path: String, text: String) {
    val psiFile = myFixture.configureFromTempProjectFile(path)
    myFixture.editor.caretModel.moveToOffset(psiFile.textLength)
    myFixture.type(text)
  }
}
