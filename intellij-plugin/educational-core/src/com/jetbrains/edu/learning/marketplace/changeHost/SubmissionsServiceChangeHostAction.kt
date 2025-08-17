package com.jetbrains.edu.learning.marketplace.changeHost

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class SubmissionsServiceChangeHostAction : ChangeServiceHostAction<SubmissionsServiceHost>(SubmissionsServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.SubmissionsServiceChangeHost"
  }
}
