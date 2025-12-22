package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.agreement.solutionSharing
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class MarketplaceSolutionSharingTest : SubmissionsTestBase() {

  override fun setUp() {
    super.setUp()
    if (MarketplaceSettings.INSTANCE.getMarketplaceAccount() == null) {
      loginFakeMarketplaceUser()
    }
    mockJBAccount(testRootDisposable)
    val settings = UserAgreementSettings.getInstance()
    val oldSharingPreference = if (settings.solutionSharing) SolutionSharingPreference.ALWAYS else SolutionSharingPreference.NEVER
    UserAgreementSettings.getInstance().setSolutionSharing(SolutionSharingPreference.ALWAYS)
    Disposer.register(testRootDisposable) {
      UserAgreementSettings.getInstance().setSolutionSharing(oldSharingPreference)
      MarketplaceSettings.INSTANCE.setAccount(null)
    }
  }

  @Test
  fun `test ShareMySolutionsAction is not visible under registry`() {
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = false)
  }

  @Test
  fun `test ShareMySolutionsAction is not visible for non-marketplace course`() {
    setMarketplaceCourse(false)
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = false)
  }

  @Test
  fun `test ShareMySolutionsAction is visible for marketplace course`() {
    setMarketplaceCourse(true)
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeEnabled = true, runAction = false)
  }

  private fun setMarketplaceCourse(state: Boolean) {
    val course = project.course ?: error("Course is null")
    course.isMarketplace = state
  }
}