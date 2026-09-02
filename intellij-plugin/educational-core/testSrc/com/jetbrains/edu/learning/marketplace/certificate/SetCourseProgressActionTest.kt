package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.ui.TestInputDialog
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestInputDialog
import com.jetbrains.edu.learning.marketplace.actions.SetCourseProgressAction
import com.jetbrains.edu.learning.marketplace.actions.SetCourseProgressAction.PercentInputValidator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withEduTestDialog
import com.jetbrains.edu.learning.withTestDialog
import org.junit.Test

class SetCourseProgressActionTest : EduActionTestCase() {

  private val service: CourseCertificatePercentageService
    get() = CourseCertificatePercentageService.getInstance(project)

  @Test
  fun `test percent is set`() {
    val dialog = withEduTestDialog(EduTestInputDialog(" 80 ")) {
      setPercent()
    }

    assertEquals(80, service.completedPercent)
    dialog.checkWasShown(EduCoreBundle.message("action.Educational.Student.SetCourseProgress.message"))
  }

  @Test
  fun `test empty input resets percent`() {
    service.completedPercent = 80

    withEduTestDialog(EduTestInputDialog("")) {
      setPercent()
    }

    assertNull(service.completedPercent)
  }

  @Test
  fun `test cancelled dialog keeps percent`() {
    service.completedPercent = 80

    withTestDialog(TestInputDialog { null }) {
      setPercent()
    }

    assertEquals(80, service.completedPercent)
  }

  @Test
  fun `test validator accepts percent`() {
    for (input in listOf("", "0", "100", " 80 ")) {
      assertNull("`$input` should be accepted", PercentInputValidator.getErrorText(input))
    }
  }

  @Test
  fun `test validator rejects invalid input`() {
    for (input in listOf("abc", "8.5", "-1", "101")) {
      assertNotNull("`$input` should be rejected", PercentInputValidator.getErrorText(input))
    }
  }

  private fun setPercent() {
    testAction(SetCourseProgressAction.ACTION_ID, SimpleDataContext.getProjectContext(project))
  }
}
