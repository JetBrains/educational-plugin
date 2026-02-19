package com.jetbrains.edu.ai.action

import com.intellij.ide.Region
import com.intellij.ide.RegionSettings
import com.jetbrains.edu.ai.EDU_AI_SERVICE_PRODUCTION_CHINA_URL
import com.jetbrains.edu.ai.host.TranslationServiceHost
import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import org.junit.runners.Parameterized

// TODO(remove isChina parameter after ai service split)
class TranslationServiceChangeHostActionTest(
  initialValue: SelectedServiceHost<TranslationServiceHost>?,
  dialogValue: SelectedServiceHost<TranslationServiceHost>?,
  private val isFromChinaRegion: Boolean,
  expectedValue: SelectedServiceHost<TranslationServiceHost>,
) : ChangeServiceHostActionTestBase<TranslationServiceHost>(initialValue, dialogValue, expectedValue) {
  override fun setUp() {
    super.setUp()
    if (isFromChinaRegion) {
      RegionSettings.setRegion(Region.CHINA)
    }
  }

  override fun tearDown() {
    try {
      if (isFromChinaRegion) {
        RegionSettings.resetCode()
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  override val manager: ServiceHostManager<TranslationServiceHost> = TranslationServiceHost
  override val actionId: String = TranslationServiceChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1} -> {2}")
    fun data(): Collection<Array<Any?>> {
      val data = TranslationServiceHost.data()

      val commonTestData = data.mapWithRegionFlag(false) { it }
      val chinaTestData = data.mapWithRegionFlag(true) { expected ->
        if (expected == SelectedServiceHost(TranslationServiceHost.default)) {
          SelectedServiceHost(TranslationServiceHost.PRODUCTION, EDU_AI_SERVICE_PRODUCTION_CHINA_URL)
        }
        else {
          expected
        }
      }

      return commonTestData + chinaTestData
    }

    private fun Collection<Array<Any?>>.mapWithRegionFlag(
      isChina: Boolean,
      transformExpected: (Any?) -> Any?
    ): List<Array<Any?>> = map { (initial, dialog, expected) ->
      arrayOf(initial, dialog, isChina, transformExpected(expected))
    }
  }
}
