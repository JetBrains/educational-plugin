package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.testAction

class MarketplaceSolutionSharingTest : SubmissionsTestBase() {

  fun `test ShareMySolutionsAction is not visible under registry`() {
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeVisible = false)
  }

  fun `test ShareMySolutionsAction is not visible for non-marketplace course`() {
    disableRegistry()
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeVisible = false)
  }

  fun `test ShareMySolutionsAction is visible for marketplace course`() {
    disableRegistry()
    setMarketplaceCourse()
    testAction(ShareMySolutionsAction.ACTION_ID)
  }

  private fun disableRegistry() = Registry.get(ShareMySolutionsAction.REGISTRY_KEY).setValue(true)

  private fun setMarketplaceCourse() {
    project.course?.isMarketplace = true
  }
}