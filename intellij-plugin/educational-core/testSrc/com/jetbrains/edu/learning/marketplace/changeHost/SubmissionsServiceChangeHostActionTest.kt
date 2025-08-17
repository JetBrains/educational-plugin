package com.jetbrains.edu.learning.marketplace.changeHost

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import org.junit.runners.Parameterized

class SubmissionsServiceChangeHostActionTest(
  initialValue: SelectedServiceHost<SubmissionsServiceHost>?,
  dialogValue: SelectedServiceHost<SubmissionsServiceHost>?,
  expectedValue: SelectedServiceHost<SubmissionsServiceHost>
) : ChangeServiceHostActionTestBase<SubmissionsServiceHost>(initialValue, dialogValue, expectedValue) {

  override val manager: ServiceHostManager<SubmissionsServiceHost> = SubmissionsServiceHost
  override val actionId: String = SubmissionsServiceChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = SubmissionsServiceHost.data()
  }
}
