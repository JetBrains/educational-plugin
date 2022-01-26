package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import org.intellij.lang.annotations.Language
import java.util.*

abstract class StepikBasedDownloadDatasetTest : EduActionTestCase() {

  @Language("JSON")
  protected val existingAttemptForTask2 = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 102,
          "status": "active",
          "step": 2,
          "time": "${Date().format()}",
          "time_left": 300,
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  protected val existingAttemptForTask3 = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 103,
          "status": "active",
          "step": 3,
          "time": "${Date().format()}",
          "time_left": 0,
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  protected val newAttemptForTask1 = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 101,
          "status": "active",
          "step": 1,
          "time": "${Date(0).format()}",
          "time_left": null,
          "user": 123
        }
      ]
    }
  """

  @Language("JSON")
  protected val noAttempts = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [ ]
    }
  """

  companion object {
    @JvmStatic
    protected val DATA_TASK_1: String = "Data Task 1"
    @JvmStatic
    protected val DATA_TASK_2: String = "Data Task 2"
    @JvmStatic
    protected val DATA_TASK_3: String = "Data Task 3"
    @JvmStatic
    protected val SOME_FILE_TXT: String = "someFile.txt"
    @JvmStatic
    protected val TASK_1_DATASET_TEXT: String = "dataset text"
    @JvmStatic
    protected val TASK_2_OLD_DATASET_TEXT: String = "old dataset for task 2 text"
    @JvmStatic
    protected val TASK_2_NEW_DATASET_TEXT: String = "new dataset for task 2 text"
    @JvmStatic
    protected val TASK_3_OLD_DATASET_TEXT: String = "old dataset for task 3 text"
    @JvmStatic
    protected val TASK_3_NEW_DATASET_TEXT: String = "new dataset for task 3 text"

//    protected fun Date.format(): String {
//      val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
//      return formatter.format(this)
//    }
  }
}