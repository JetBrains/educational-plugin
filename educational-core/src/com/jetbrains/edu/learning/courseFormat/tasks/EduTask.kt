package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import org.jetbrains.annotations.NonNls
import java.util.*

/**
 * Original Edu plugin tasks with local tests and answer placeholders
 */
open class EduTask : Task {
  constructor()
  constructor(name: String) : super(name)
  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) :
    super(name, id, position, updateDate, status)

  override val itemType: String = EDU_TASK_TYPE

  override val isToSubmitToRemote: Boolean
    get() = checkStatus != CheckStatus.Unchecked

  override val supportSubmissions: Boolean
    get() = true

  companion object {
    @NonNls
    const val EDU_TASK_TYPE: String = "edu"

    @NonNls
    const val PYCHARM_TASK_TYPE: String = "pycharm"
  }
}