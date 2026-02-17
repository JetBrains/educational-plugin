package com.jetbrains.edu.lti.changeHost

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class LTIServiceChangeHostAction : ChangeServiceHostAction<LTIServiceHost>(LTIServiceHost) {

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.LTIServiceChangeHost"
  }
}