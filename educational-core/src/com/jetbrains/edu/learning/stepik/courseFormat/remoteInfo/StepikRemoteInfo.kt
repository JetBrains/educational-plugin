package com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import java.util.*

open class StepikRemoteInfo : RemoteInfo  {
  var updateDate = Date(0)
  var id: Int = 0
}

class StepikTaskRemoteInfo : StepikRemoteInfo()