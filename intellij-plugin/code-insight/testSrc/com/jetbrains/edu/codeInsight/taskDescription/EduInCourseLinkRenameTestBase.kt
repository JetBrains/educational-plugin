package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

abstract class EduInCourseLinkRenameTestBase(format: DescriptionFormat) : EduTaskDescriptionTestBase(format) {

  override fun createCourse() {
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1", taskDescriptionFormat = taskDescriptionFormat) {
          taskFile("TaskFile1.txt")
          taskFile("foo/bar/TaskFile2.txt")
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("task2", taskDescriptionFormat = taskDescriptionFormat) {
            taskFile("TaskFile3.txt")
          }
        }
      }
    }
  }

  @Test
  fun `test rename section`() = doTest("section 1", "course://section<caret>1", "course://section 1") {
    dir("lesson1/task1") {
      file("TaskFile1.txt")
      dir("foo/bar") {
        file("TaskFile2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section 1/lesson2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  @Test
  fun `test rename lesson`() = doTest("lesson 1", "course://lesson<caret>1", "course://lesson 1") {
    dir("lesson 1/task1") {
      file("TaskFile1.txt")
      dir("foo/bar") {
        file("TaskFile2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section1/lesson2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  @Test
  fun `test rename lesson in section`() = doTest("lesson 2", "course://section1/lesson<caret>2", "course://section1/lesson 2") {
    dir("lesson1/task1") {
      file("TaskFile1.txt")
      dir("foo/bar") {
        file("TaskFile2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section1/lesson 2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  @Test
  fun `test rename task`() = doTest("task 1", "course://lesson1/task<caret>1", "course://lesson1/task 1") {
    dir("lesson1/task 1") {
      file("TaskFile1.txt")
      dir("foo/bar") {
        file("TaskFile2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section1/lesson2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  @Test
  fun `test rename task file`() = doTest(
    "TaskFile 1.txt",
    "course://lesson1/task1/TaskFile1<caret>.txt",
    "course://lesson1/task1/TaskFile 1.txt"
  ) {
    dir("lesson1/task1") {
      file("TaskFile 1.txt")
      dir("foo/bar") {
        file("TaskFile2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section1/lesson2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  @Test
  fun `test rename task file in directory`() = doTest(
    "TaskFile 2.txt",
    "course://lesson1/task1/foo/bar/TaskFile2<caret>.txt",
    "course://lesson1/task1/foo/bar/TaskFile 2.txt"
  ) {
    dir("lesson1/task1") {
      file("TaskFile1.txt")
      dir("foo/bar") {
        file("TaskFile 2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section1/lesson2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  @Test
  fun `test rename item in the middle of link`() = doTest(
    "task 1",
    "course://lesson1/task<caret>1/TaskFile1.txt",
    "course://lesson1/task 1/TaskFile1.txt"
  ) {
    dir("lesson1/task 1") {
      file("TaskFile1.txt")
      dir("foo/bar") {
        file("TaskFile2.txt")
      }
      file(taskDescriptionFormat.descriptionFileName)
    }
    dir("section1/lesson2/task2") {
      file("TaskFile3.txt")
      file(taskDescriptionFormat.descriptionFileName)
    }
  }

  protected open fun doTest(newName: String, linkBefore: String, linkAfter: String, expectedFileTree: FileTreeBuilder.() -> Unit) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")

    myFixture.saveText(taskDescriptionFile, linkBefore.withDescriptionFormat())
    myFixture.configureFromExistingVirtualFile(taskDescriptionFile)

    myFixture.renameElementAtCaret(newName)

    myFixture.checkResult(linkAfter.withDescriptionFormat())
    fileTree(expectedFileTree).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }
}
