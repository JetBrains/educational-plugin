package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduActionTestCase
import org.intellij.lang.annotations.Language
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

abstract class StepikBasedDownloadDatasetTest : EduActionTestCase() {
  private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
  private fun now() = ZonedDateTime.now(ZoneId.of("GMT0")).format(format)

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
          "time": "${now()}",
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
          "time": "${now()}",
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
          "time": "${now()}",
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
  }
}