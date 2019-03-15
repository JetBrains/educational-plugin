package com.jetbrains.edu.learning.actions

import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.withTestDialog
import java.io.IOException

class RefreshTaskTest : EduTestCase() {

  fun testRefreshTask() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.type("test")
    withTestDialog(EduTestDialog(Messages.OK)) {
      myFixture.testAction(RevertTaskAction())
    }

    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  fun testCaretOutside() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(4)
    myFixture.type("test")
    withTestDialog(EduTestDialog(Messages.OK)) {
      myFixture.testAction(RevertTaskAction())
    }
    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  fun testMultipleFiles() {
    configureByTaskFile(1, 3, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(5)
    myFixture.type("test")
    configureByTaskFile(1, 3, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(5)
    myFixture.type("test")
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    withTestDialog(EduTestDialog(Messages.OK)) {
      myFixture.testAction(RevertTaskAction())
    }
    val fileName = "lesson1/task3/taskFile1.txt"
    val fileName2 = "lesson1/task3/taskFile2.txt"
    val file1 = myFixture.findFileInTempDir(fileName)
    val file2 = myFixture.findFileInTempDir(fileName2)
    myFixture.openFileInEditor(file1)
    assertEquals("Look! There is my placeholder.", myFixture.editor.document.text)
    myFixture.openFileInEditor(file2)
    assertEquals("Look! There is placeholder.", myFixture.editor.document.text)
  }

  @Throws(IOException::class)
  override fun createCourse() {

    StudyTaskManager.getInstance(myFixture.project).course = courseWithFiles {
      lesson {
        eduTask {
          taskFile(
            "taskFile1.txt", """
          Look! There <p>is</p> placeholder.
        """
          )
        }
        eduTask {
          taskFile("taskFile2.txt", """
          Look! There <p>is</p> placeholder.
        """
          )
        }
        eduTask {
          taskFile("taskFile1.txt", """
          Look! There <p>is</p> my placeholder.
        """)
          taskFile("taskFile2.txt", """
          Look! There <p>is</p> placeholder.
        """
          )
        }
      }
    }
  }

}
