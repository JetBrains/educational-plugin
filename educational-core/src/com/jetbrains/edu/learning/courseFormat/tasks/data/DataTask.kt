package com.jetbrains.edu.learning.courseFormat.tasks.data

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.NonNls
import java.util.*

class DataTask : Task {
  @get: Synchronized
  @set: Synchronized
  var attempt: DataTaskAttempt? = null

  @get: Synchronized
  val isTimeLimited: Boolean
    get() = attempt?.endDateTime != null

  //used for deserialization
  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val itemType: String = DATA_TASK_TYPE

  override val supportSubmissions: Boolean
    get() = false

  fun isRunning(): Boolean {
    if (status != CheckStatus.Unchecked) return false
    return attempt?.isRunning ?: false
  }



  companion object {
    @NonNls
    const val DATA_TASK_TYPE: String = "dataset"

    @NonNls
    const val DATA_FOLDER_NAME: String = "data"

    @NonNls
    const val DATA_SAMPLE_FOLDER_NAME: String = "sample"

    @NonNls
    const val DATASET_FOLDER_NAME: String = "dataset"

    @NonNls
    const val INPUT_FILE_NAME: String = "input.txt"
  }
}