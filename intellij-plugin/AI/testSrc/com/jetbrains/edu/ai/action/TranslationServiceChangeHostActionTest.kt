package com.jetbrains.edu.ai.action

import com.jetbrains.edu.ai.host.TranslationServiceHost
import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import org.junit.runners.Parameterized

class TranslationServiceChangeHostActionTest(
  initialValue: SelectedServiceHost<TranslationServiceHost>?,
  dialogValue: SelectedServiceHost<TranslationServiceHost>?,
  expectedValue: SelectedServiceHost<TranslationServiceHost>,
) : ChangeServiceHostActionTestBase<TranslationServiceHost>(initialValue, dialogValue, expectedValue) {
  override val manager: ServiceHostManager<TranslationServiceHost> = TranslationServiceHost
  override val actionId: String = TranslationServiceChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = TranslationServiceHost.data()
  }
}
