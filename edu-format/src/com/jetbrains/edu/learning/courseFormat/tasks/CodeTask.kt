package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

class CodeTask : Task {
  //used for deserialization
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus, submissionLanguage: String? = null) :
    super(name, id, position, updateDate, status) {
      this.submissionLanguage = submissionLanguage
    }

  var submissionLanguage: String? = null
  override val itemType: String = CODE_TASK_TYPE

  override val supportSubmissions: Boolean
    get() = true

  override val isPluginTaskType: Boolean
    get() = false

  companion object {
    const val CODE_TASK_TYPE: String = "code"
  }
}