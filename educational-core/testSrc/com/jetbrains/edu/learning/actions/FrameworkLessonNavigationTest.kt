package com.jetbrains.edu.learning.actions

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.NavigationAction.NEXT
import com.jetbrains.edu.learning.actions.NavigationAction.PREV
import com.jetbrains.edu.learning.courseFormat.Course

class FrameworkLessonNavigationTest : EduTestCase() {

  private val rootDir: VirtualFile get() = LightPlatformTestCase.getSourceRoot()

  fun `test next`() = doTest("lesson1/task/file-1.txt", NEXT) {
    dir("lesson1") {
      dir("task") {
        file("file-1.txt")
        file("file-2.txt")
      }
    }
  }

  fun `test next next`() = doTest("lesson1/task/file-1.txt", NEXT, NEXT) {
    dir("lesson1") {
      dir("task") {
        file("file-3.txt")
      }
    }
  }

  fun `test next prev`() = doTest("lesson1/task/file-1.txt", NEXT, PREV) {
    dir("lesson1") {
      dir("task") {
        file("file-1.txt")
      }
    }
  }

  private fun doTest(fileToOpen: String, vararg actions: NavigationAction, block: FileTreeBuilder.() -> Unit) {
    createFrameworkCourse()
    val file = rootDir.findFileByRelativePath(fileToOpen) ?: error("Can't find `$fileToOpen` file")
    myFixture.openFileInEditor(file)

    for (action in actions) {
      myFixture.testAction(action.action())
    }

    fileTree(block).assertEquals(rootDir)
  }

  private fun createFrameworkCourse(): Course = courseWithFiles {
    lesson(isFramework = true) {
      eduTask {
        taskFile("file-1.txt", "")
      }
      eduTask {
        taskFile("file-1.txt", "")
        taskFile("file-2.txt", "")
      }
      eduTask {
        taskFile("file-3.txt", "")
      }
    }
  }
}

private enum class NavigationAction {
  NEXT, PREV;

  fun action(): TaskNavigationAction = when (this) {
    NEXT -> NextTaskAction()
    PREV -> PreviousTaskAction()
  }
}
