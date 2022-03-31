package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.annotations.NonNls
import java.util.*

/**
 * By default, EduTask is checked with local tests, but if `is_remote_tested` flag is true, then we should send code to remote check
 */
// see EDU-4504 Support Go edu problems
class RemoteEduTask : EduTask {
  constructor()
  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  /**
   * This is name of docker image used by Hyperskill to check task
   * MUST NOT BE HARDCODED
   */
  var checkProfile: String = ""

  override val itemType: String = REMOTE_EDU_TASK_TYPE

  companion object {
    @NonNls
    const val REMOTE_EDU_TASK_TYPE: String = "remote_edu"
  }
}