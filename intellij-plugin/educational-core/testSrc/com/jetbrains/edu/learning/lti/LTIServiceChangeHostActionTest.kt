package com.jetbrains.edu.learning.lti

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import com.jetbrains.edu.learning.marketplace.lti.changeHost.LTIServiceChangeHostAction
import com.jetbrains.edu.learning.marketplace.lti.changeHost.LTIServiceHost
import org.junit.runners.Parameterized

class LTIServiceChangeHostActionTest(
  initialValue: SelectedServiceHost<LTIServiceHost>?,
  dialogValue: SelectedServiceHost<LTIServiceHost>?,
  expectedValue: SelectedServiceHost<LTIServiceHost>
) : ChangeServiceHostActionTestBase<LTIServiceHost>(initialValue, dialogValue, expectedValue) {

  override val manager: ServiceHostManager<LTIServiceHost> = LTIServiceHost.Companion
  override val actionId: String = LTIServiceChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = LTIServiceHost.data()
  }
}
