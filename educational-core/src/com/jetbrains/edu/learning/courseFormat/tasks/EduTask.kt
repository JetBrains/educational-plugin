package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

/**
 * Original Edu plugin tasks with local tests and answer placeholders
 */
open class EduTask : Task {
  constructor()
  constructor(name: String) : super(name)
  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) :
    super(name, id, position, updateDate, status)

  override fun getItemType(): String = EDU_TASK_TYPE

  override fun isToSubmitToRemote(): Boolean {
    return myStatus != CheckStatus.Unchecked
  }

  override fun supportSubmissions(): Boolean = true

  companion object {
    @NonNls
    const val EDU_TASK_TYPE: String = "edu"

    @NonNls
    const val PYCHARM_TASK_TYPE: String = "pycharm"
  }
}