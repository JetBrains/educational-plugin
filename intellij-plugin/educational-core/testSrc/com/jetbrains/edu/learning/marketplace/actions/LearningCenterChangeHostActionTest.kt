package com.jetbrains.edu.learning.marketplace.actions

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import com.jetbrains.edu.learning.marketplace.changeHost.LearningCenterChangeHostAction
import com.jetbrains.edu.learning.marketplace.changeHost.LearningCenterServiceHost
import org.junit.runners.Parameterized

class LearningCenterChangeHostActionTest(
  initialValue: SelectedServiceHost<LearningCenterServiceHost>?,
  dialogValue: SelectedServiceHost<LearningCenterServiceHost>?,
  expectedValue: SelectedServiceHost<LearningCenterServiceHost>
) : ChangeServiceHostActionTestBase<LearningCenterServiceHost>(initialValue, dialogValue, expectedValue) {

  override val manager: ServiceHostManager<LearningCenterServiceHost> = LearningCenterServiceHost
  override val actionId: String = LearningCenterChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = LearningCenterServiceHost.data()
  }
}