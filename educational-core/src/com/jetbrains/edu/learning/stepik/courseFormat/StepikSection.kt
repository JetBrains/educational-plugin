package com.jetbrains.edu.learning.stepik.courseFormat

import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikSectionRemoteInfo

class StepikSection : Section() {

  val stepikRemoteInfo: StepikSectionRemoteInfo
    get() {
      val info = super.getRemoteInfo()
      assert(info is StepikSectionRemoteInfo)
      return info as StepikSectionRemoteInfo
    }

  init {
    remoteInfo = StepikSectionRemoteInfo()
  }
}
