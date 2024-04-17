package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.TextRange
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class InvisibleDependencyTest : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  @Test
  fun `test invisible placeholder with invisible dependency`() = doTest(CheckStatus.Solved, false, "\"Foo\"")
  @Test
  fun `test visible placeholder with invisible dependency`() = doTest(CheckStatus.Unchecked, true, "type Foo")

  private fun doTest(status: CheckStatus, expectedVisibility: Boolean, expectedPlaceholderText: String) {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", """
          fun foo(): String = <p>TODO()</p>
        """) {
            placeholder(0, "\"Foo\"")
          }
        }
        eduTask("task2") {
          taskFile("Task.kt", """
          fun foo2(): String = <p>type Foo</p>
          fun bar(): String = <p>type Bar</p>
        """) {
            placeholder(0, "\"Foo\"", dependency = "lesson1#task1#Task.kt#1", isVisible = false)
          }
        }
      }
    }

    createCourseStructure(course)
    createConfigFiles(project)
    EduDocumentListener.setGlobalListener(project, testRootDisposable)

    val fileEditorManager = FileEditorManager.getInstance(project)

    val task1 = course.findTask("lesson1", "task1")
    val task1File = task1.getTaskFile("Task.kt")!!
    val task1VirtualFile = task1File.getVirtualFile(project)!!
    val textEditor1 = fileEditorManager.openTextEditor(OpenFileDescriptor(project, task1VirtualFile), true)!!

    val placeholder1 = task1File.answerPlaceholders[0]
    WriteCommandAction.runWriteCommandAction(project) {
      textEditor1.document.replaceString(placeholder1.offset, placeholder1.endOffset, "\"Foo\"")
    }

    task1.status = status

    val task2 = course.findTask("lesson1", "task2")
    val task2File = task2.getTaskFile("Task.kt")!!
    val task2VirtualFile = task2File.getVirtualFile(project)!!
    val textEditor2 = fileEditorManager.openTextEditor(OpenFileDescriptor(project, task2VirtualFile), true)!!

    val placeholder2 = task2File.answerPlaceholders[0]
    assertEquals(expectedVisibility, placeholder2.isCurrentlyVisible)
    assertEquals(expectedPlaceholderText, textEditor2.document.getText(TextRange(placeholder2.offset, placeholder2.endOffset)))

    fileEditorManager.closeFile(task1VirtualFile)
    fileEditorManager.closeFile(task2VirtualFile)
  }
}
