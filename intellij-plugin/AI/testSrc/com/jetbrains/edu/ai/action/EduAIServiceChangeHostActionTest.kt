package com.jetbrains.edu.ai.action

import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import org.junit.runners.Parameterized

class EduAIServiceChangeHostActionTest(
  initialValue: SelectedServiceHost<EduAIServiceHost>?,
  dialogValue: SelectedServiceHost<EduAIServiceHost>?,
  expectedValue: SelectedServiceHost<EduAIServiceHost>
) : ChangeServiceHostActionTestBase<EduAIServiceHost>(initialValue, dialogValue, expectedValue) {

  override val manager: ServiceHostManager<EduAIServiceHost> = EduAIServiceHost
  override val actionId: String = EduAIServiceChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = EduAIServiceHost.data()
  }
}
