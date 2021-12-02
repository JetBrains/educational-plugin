package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

class CodeTask : Task {
  //used for deserialization
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) :
    super(name, id, position, updateDate, status)

  override fun getItemType(): String = CODE_TASK_TYPE

  override fun supportSubmissions(): Boolean = true

  override fun isPluginTaskType(): Boolean = false

  companion object {
    const val CODE_TASK_TYPE: String = "code"
  }
}