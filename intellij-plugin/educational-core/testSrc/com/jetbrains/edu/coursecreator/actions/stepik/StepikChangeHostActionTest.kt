package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.StepikNames.COGNITERRA_URL
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_DEFAULT_URL
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_HOST_ORDINAL_PROPERTY
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_RELEASE_URL
import com.jetbrains.edu.learning.stepik.StepikNames.getClientId
import com.jetbrains.edu.learning.stepik.StepikNames.getClientSecret
import com.jetbrains.edu.learning.stepik.StepikOAuthBundle
import com.jetbrains.edu.learning.stepik.changeHost.StepikChangeHost
import com.jetbrains.edu.learning.stepik.changeHost.StepikChangeHostUI
import com.jetbrains.edu.learning.stepik.changeHost.StepikHost
import com.jetbrains.edu.learning.stepik.changeHost.withMockStepikChangeHostUI
import com.jetbrains.edu.learning.testAction

class StepikChangeHostActionTest : EduTestCase() {
  private var initialStepikOrdinal: Int = 0

  override fun setUp() {
    super.setUp()
    // we can not use STEPIK_URL property directly here because it returns "https://release.stepik.org"
    // in unit test mode, which is needed for other tests
    initialStepikOrdinal = StepikHost.getSelectedHost().ordinal
    PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, StepikHost.PRODUCTION.ordinal, 0)
  }

  override fun tearDown() {
    try {
      PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, initialStepikOrdinal, 0)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun `test cogniterra stepik host`() {
    PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, StepikHost.COGNITERRA.ordinal, 2)
    doTestStepikHostChanged(COGNITERRA_URL, StepikHost.COGNITERRA, StepikOAuthBundle.value("cogniterraClientId"),
                            StepikOAuthBundle.value("cogniterraClientSecret"))
  }
  fun `test default stepik host`() {
    PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, StepikHost.RELEASE.ordinal, 4)
    doTestStepikHostChanged(STEPIK_RELEASE_URL, StepikHost.PRODUCTION, StepikOAuthBundle.value("stepikClientId"),
                            StepikOAuthBundle.value("stepikClientSecret"))
  }

  fun `test host changed to RELEASE`() {
    doTestStepikHostChanged(STEPIK_DEFAULT_URL, StepikHost.RELEASE, StepikOAuthBundle.value("stepikNonProductionClientId"),
                            StepikOAuthBundle.value("stepikNonProductionClientSecret"))
  }


  private fun doChangeStepikHostAction(stepikHost: StepikHost) {
    withMockStepikChangeHostUI(object : StepikChangeHostUI {
      override fun showDialog(): StepikHost {
        return stepikHost
      }
    }) {
      testAction(StepikChangeHost.ACTION_ID)
    }
  }

  private fun doTestStepikHostChanged(initialHost: String, newHost: StepikHost, expectedClientId: String, expectedClientSecret: String) {
    // we can not use STEPIK_URL property directly here because it returns "https://release.stepik.org"
    // in unit test mode, which is needed for other tests
    assertEquals(initialHost, StepikHost.getSelectedHost().url)
    doChangeStepikHostAction(newHost)
    assertEquals(newHost.url, StepikHost.getSelectedHost().url)
    assertEquals(expectedClientId, getClientId())
    assertEquals(expectedClientSecret, getClientSecret())
  }
}