package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import org.junit.Test

class CodeforcesChooseFileForRunConfigurationTest : EduActionTestCase() {
  @Test
  fun `test first test set is selected`() {
    doTest(selectedFileName = "$TASK1/$TEST_DATA1", folderForDebug = "$TASK1/$TEST_DATA1/$INPUT")
  }

  @Test
  fun `test second test set is selected`() {
    doTest(selectedFileName = "$TASK1/$TEST_DATA2/$OUTPUT", folderForDebug = "$TASK1/$TEST_DATA2/$INPUT")
  }

  @Test
  fun `test test data root is selected with the only valid test set`() {
    doTest(selectedFileName = "$TASK2/${CodeforcesNames.TEST_DATA_FOLDER}", folderForDebug = "$TASK2/$TEST_DATA1/$INPUT")
  }

  @Test
  fun `test invalid selection, broken test set is selected`() {
    doTest("$TASK2/$TEST_DATA2")
  }

  @Test
  fun `test invalid selection, task root is selected`() {
    doTest(TASK2)
  }

  private fun doTest(selectedFileName: String, folderForDebug: String? = null) {
    getCodeforcesCourse()
    val expectedFile = if (folderForDebug == null) null else myFixture.findFileInTempDir(folderForDebug)!!
    val selectedFile = myFixture.findFileInTempDir(selectedFileName) ?: error("Unable to find selected file")
    val actualInputFile = CodeforcesUtils.getInputFile(project, selectedFile)
    assertEquals(expectedFile, actualInputFile)
  }

  private fun getCodeforcesCourse(): CodeforcesCourse {
    return courseWithFiles(courseProducer = ::CodeforcesCourse) {
      lesson(CodeforcesNames.CODEFORCES_PROBLEMS) {
        codeforcesTask {
          taskFile(TASK_FILE)
          taskFile("$TEST_DATA1/$INPUT")
          taskFile("$TEST_DATA1/$OUTPUT")
          taskFile("$TEST_DATA2/$INPUT")
          taskFile("$TEST_DATA2/$OUTPUT")
        }
        codeforcesTask {
          taskFile(TASK_FILE)
          taskFile("$TEST_DATA1/$INPUT")
          taskFile("$TEST_DATA1/$OUTPUT")
          taskFile("$TEST_DATA2/foo.kt")
        }
      }
    } as CodeforcesCourse
  }

  companion object {
    private const val NAME1: String = "task1"
    private const val NAME2: String = "task2"
    private const val TASK1: String = "${CodeforcesNames.CODEFORCES_PROBLEMS}/$NAME1"
    private const val TASK2: String = "${CodeforcesNames.CODEFORCES_PROBLEMS}/$NAME2"
    private const val TEST_DATA1: String = "${CodeforcesNames.TEST_DATA_FOLDER}/1"
    private const val TEST_DATA2: String = "${CodeforcesNames.TEST_DATA_FOLDER}/2"
    private const val TASK_FILE: String = "src/Task.kt"
    private const val INPUT: String = "input.txt"
    private const val OUTPUT: String = "output.txt"
  }
}
