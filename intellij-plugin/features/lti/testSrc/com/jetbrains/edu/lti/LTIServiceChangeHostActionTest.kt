package com.jetbrains.edu.lti

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.lti.changeHost.LTIServiceChangeHostAction
import com.jetbrains.edu.lti.changeHost.LTIServiceHost
import org.junit.runners.Parameterized

class LTIServiceChangeHostActionTest(
  initialValue: ServiceHostManager.SelectedServiceHost<LTIServiceHost>?,
  dialogValue: ServiceHostManager.SelectedServiceHost<LTIServiceHost>?,
  expectedValue: ServiceHostManager.SelectedServiceHost<LTIServiceHost>
) : ChangeServiceHostActionTestBase<LTIServiceHost>(initialValue, dialogValue, expectedValue) {

  override val manager: ServiceHostManager<LTIServiceHost> = LTIServiceHost.Companion
  override val actionId: String = LTIServiceChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = LTIServiceHost.data()
  }
}