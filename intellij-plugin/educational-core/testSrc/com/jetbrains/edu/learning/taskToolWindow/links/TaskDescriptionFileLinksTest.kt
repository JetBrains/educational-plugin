package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.fileEditor.FileEditorManager
import org.junit.Test

class TaskDescriptionFileLinksTest : TaskDescriptionLinksTestBase() {

  @Test
  fun `test top-level file`() = doTest(filePath = "additionalFile.txt", linkText = "file://additionalFile.txt")
  @Test
  fun `test file in directory`() = doTest(filePath = "fooBar/additionalFile.txt", linkText = "file://fooBar/additionalFile.txt")

  @Test
  fun `test top-level file with space in path`() = doTest(filePath = "additional file.txt", linkText = "file://additional%20file.txt")
  @Test
  fun `test file in directory with space in path 1`() = doTest(filePath = "foo bar/additionalFile.txt", linkText = "file://foo%20bar/additionalFile.txt")
  @Test
  fun `test file in directory with space in path 2`() = doTest(filePath = "foo bar/additional file.txt", linkText = "file://foo%20bar/additional%20file.txt")

  private fun doTest(filePath: String, linkText: String) {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt")
        }
      }
      additionalFile(filePath)
    }

    openLink(linkText)
    val file = findFile(filePath)
    val openFiles = FileEditorManager.getInstance(project).openFiles
    check(file in openFiles) {
      "$file should be opened"
    }
  }
}
