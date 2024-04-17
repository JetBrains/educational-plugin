package com.jetbrains.edu.learning.codeforces.run

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import org.junit.Test

class CodeforcesRunLineMarkerContributorTest : EduTestCase() {
  @Test
  fun `test one line marker in input file`() {
    createCodeforcesCourse()
    myFixture.openFileInEditor(findFileInTask(0, 0, "testData/1/input.txt"))
    assertEquals(1, getLineMarkers().size)
  }

  @Test
  fun `test no line marker present in output file`() {
    createCodeforcesCourse()
    myFixture.openFileInEditor(findFileInTask(0, 0, "testData/1/output.txt"))
    assertEquals(0, getLineMarkers().size)
  }

  @Test
  fun `test no line marker present in task file`() {
    createCodeforcesCourse()
    myFixture.openFileInEditor(findFileInTask(0, 0, "task.txt"))
    assertEquals(0, getLineMarkers().size)
  }

  @Test
  fun `test no line marker when output file is missing`() {
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile("task.txt")
          taskFile("testData/1/input.txt")
        }
      }
    }
    myFixture.openFileInEditor(findFileInTask(0, 0, "testData/1/input.txt"))
    assertEquals(0, getLineMarkers().size)
  }

  @Test
  fun `test no line marker in non codeforces task`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt")
          taskFile("testData/1/input.txt")
          taskFile("testData/1/output.txt")
        }
      }
    }
    myFixture.openFileInEditor(findFileInTask(0, 0, "testData/1/output.txt"))
    assertEquals(0, getLineMarkers().size)
  }

  private fun createCodeforcesCourse() {
    courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask {
          taskFile("task.txt")
          taskFile("testData/1/input.txt")
          taskFile("testData/1/output.txt")
        }
      }
    }
  }

  private fun getLineMarkers(): List<LineMarkerInfo<*>> {
    myFixture.doHighlighting()
    return DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, myFixture.project)
  }
}