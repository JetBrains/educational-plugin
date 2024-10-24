package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.StepikNames.COGNITERRA_URL
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_HOST_ORDINAL_PROPERTY
import com.jetbrains.edu.learning.stepik.StepikNames.getClientId
import com.jetbrains.edu.learning.stepik.StepikOAuthBundle
import com.jetbrains.edu.learning.stepik.changeHost.StepikChangeHost
import com.jetbrains.edu.learning.stepik.changeHost.StepikChangeHostUI
import com.jetbrains.edu.learning.stepik.changeHost.StepikHost
import com.jetbrains.edu.learning.stepik.changeHost.withMockStepikChangeHostUI
import com.jetbrains.edu.learning.testAction
import org.junit.Test

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

  @Test
  fun `test cogniterra stepik host`() {
    PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, StepikHost.COGNITERRA.ordinal, 2)
    assertEquals(COGNITERRA_URL, StepikHost.getSelectedHost().url)
    withMockStepikChangeHostUI(object : StepikChangeHostUI {
      override fun showDialog(): StepikHost {
        return StepikHost.COGNITERRA
      }
    }) {
      testAction(StepikChangeHost.ACTION_ID)
    }
    assertEquals(StepikHost.COGNITERRA.url, StepikHost.getSelectedHost().url)
    assertEquals(StepikOAuthBundle.value("cogniterraClientId"), getClientId())
  }
}