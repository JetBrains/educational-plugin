package com.jetbrains.edu.learning.actions.refresh

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.yaml.YamlDeepLoader
import org.junit.Test
import org.junit.Assert.assertArrayEquals
import java.io.IOException
import kotlin.test.assertContains

class RefreshTaskTest : EduTestCase() {

  @Test
  fun testRefreshTask() {
    // Given
    configureByTaskFile(1, 1, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.type("test")

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    // Then
    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  @Test
  fun testCaretOutside() {
    // Given
    configureByTaskFile(1, 2, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(4)
    myFixture.type("test")

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    // Then
    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  @Test
  fun testMultipleFiles() {
    // Given
    configureByTaskFile(1, 3, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(5)
    myFixture.type("test")
    configureByTaskFile(1, 3, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(5)
    myFixture.type("test")
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }
    val fileName = "lesson1/task3/taskFile1.txt"
    val fileName2 = "lesson1/task3/taskFile2.txt"
    val file1 = myFixture.findFileInTempDir(fileName)
    val file2 = myFixture.findFileInTempDir(fileName2)

    // Then
    myFixture.openFileInEditor(file1)
    assertEquals("Look! There is my placeholder.", myFixture.editor.document.text)
    myFixture.openFileInEditor(file2)
    assertEquals("Look! There is placeholder.", myFixture.editor.document.text)
  }

  @Test
  fun `test invisible files`() {
    // Given
    // Emulate load course from yaml after project reopening
    StudyTaskManager.getInstance(project).course = YamlDeepLoader.loadCourse(project)

    configureByTaskFile(1, 4, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(0)
    myFixture.type("test")
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    val taskDir = findFile("lesson1/task4")

    // Then
    fileTree {
      file("taskFile1.txt", "TaskFile1")
      file("taskFile2.txt", "TaskFile2")
      file("task.md")
      file("task-info.yaml")
    }.assertEquals(taskDir, myFixture)
  }

  @Test
  fun `untouched binary task file does not change after refresh`() {
    // Given
    configureByTaskFile(1, 5, "taskFile1.txt")
    val binaryFile = myFixture.findFileInTempDir("lesson1/task5/image.bin")
    val initialBytes = byteArrayOf(10, 20, 30, 40)
    assertArrayEquals(initialBytes, binaryFile.contentsToByteArray())

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    // Then
    assertArrayEquals(initialBytes, binaryFile.contentsToByteArray())
  }

  @Test
  fun `changed binary task file is refreshed to initial version`() {
    // Given
    configureByTaskFile(1, 6, "taskFile1.txt")
    val binaryFile = myFixture.findFileInTempDir("lesson1/task6/image.bin")
    val initialBytes = byteArrayOf(1, 2, 3, 4)
    val changedBytes = byteArrayOf(9, 8, 7, 6)

    runWriteAction {
      binaryFile.setBinaryContent(changedBytes)
    }
    assertArrayEquals(changedBytes, binaryFile.contentsToByteArray())

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    // Then
    assertArrayEquals(initialBytes, binaryFile.contentsToByteArray())
  }

  @Test
  fun `task dir macro is substituted on course creation and refresh`() {
    // Given
    configureByTaskFile(1, 7, "taskFile1.txt")
    val runConfigurationFile = myFixture.findFileInTempDir("lesson1/task7/runConfigurations/task.run.xml")
    val expectedTaskDir = $$"$PROJECT_DIR$/lesson1/task7"
    val taskDirMacro = $$"$TASK_DIR$"

    val createdText = VfsUtil.loadText(runConfigurationFile)
    assertContains(createdText, "$expectedTaskDir/src/Task.kt")
    assertFalse($$"Run configuration should not contain macro $TASK_DIR$ after the course is created", createdText.contains(taskDirMacro))

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    // Then
    val refreshedText = VfsUtil.loadText(runConfigurationFile)
    assertContains(refreshedText, "$expectedTaskDir/src/Task.kt")
    assertFalse($$"Run configuration should not contain macro $TASK_DIR$ after the task is reset", refreshedText.contains(taskDirMacro))
  }

  @Test
  fun `content of test file stays empty after refresh`() {
    // Given
    configureByTaskFile(1, 8, "taskFile1.txt")
    val testsFile = myFixture.findFileInTempDir("lesson1/task8/tests/Tests.txt")
    assertEquals("", VfsUtil.loadText(testsFile))

    // When
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    // Then
    assertEquals("", VfsUtil.loadText(testsFile))
  }

  @Throws(IOException::class)
  override fun createCourse() {
    StudyTaskManager.getInstance(myFixture.project).course = courseWithFiles(
      createYamlConfigs = true,
      courseProducer = { EduCourse().apply { isMarketplace = true }}
    ) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt", """
            Look! There <p>is</p> placeholder.
          """)
        }
        eduTask {
          taskFile("taskFile2.txt", """
            Look! There <p>is</p> placeholder.
          """)
        }
        eduTask {
          taskFile("taskFile1.txt", """
            Look! There <p>is</p> my placeholder.
          """)
          taskFile("taskFile2.txt", """
            Look! There <p>is</p> placeholder.
          """)
        }
        eduTask {
          taskFile("taskFile1.txt", "TaskFile1")
          taskFile("taskFile2.txt", "TaskFile2", visible = false)
        }
        eduTask {
          taskFile("taskFile1.txt", "Task file")
          taskFile("image.bin", InMemoryBinaryContents(byteArrayOf(10, 20, 30, 40)))
        }
        eduTask {
          taskFile("taskFile1.txt", "Task file")
          taskFile("image.bin", InMemoryBinaryContents(byteArrayOf(1, 2, 3, 4)))
        }
        eduTask {
          taskFile("taskFile1.txt", "Task file")
          taskFile("src/Task.kt", "fun task() = Unit")
          taskFile("runConfigurations/task.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="TaskKt" type="JetRunConfigurationType">
                <option name="WORKING_DIRECTORY" value="$TASK_DIR$/src/Task.kt" />
              </configuration>
            </component>
          """.trimIndent(), visible = false)
        }
        eduTask {
          taskFile("taskFile1.txt", "Visible file")
          taskFile("tests/Tests.txt", "Some contents of a test file invisible for learners", visible = false)
        }
      }
    }
  }

}
