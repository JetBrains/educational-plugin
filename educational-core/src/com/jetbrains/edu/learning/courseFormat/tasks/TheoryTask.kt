package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle.lazyMessage
import java.util.*

open class TheoryTask : Task {
  // needed to prohibit post empty submission at unsupported tasks opening (sorting, matching, text, etc)
  @JvmField
  var postSubmissionOnOpen = true

  //used for deserialization
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) :
    super(name, id, position, updateDate, status)

  override val itemType: String = THEORY_TASK_TYPE

  override val checkAction: CheckAction
    get() = CheckAction(lazyMessage("action.check.run.text"), lazyMessage("action.check.run.description"))

  companion object {
    const val THEORY_TASK_TYPE: String = "theory"
  }
}