package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.stepik.SubmissionsTestBase
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withRegistryKeyOff

class MarketplaceSolutionSharingTest : SubmissionsTestBase() {

  override fun setUp() {
    super.setUp()
    val registryValue = Registry.get(ShareMySolutionsAction.REGISTRY_KEY)
    val oldValue = registryValue.asBoolean()
    registryValue.setValue(true)
    Disposer.register(testRootDisposable) {
      registryValue.setValue(oldValue)
    }
  }

  fun `test ShareMySolutionsAction is not visible under registry`() {
    withRegistryKeyOff(ShareMySolutionsAction.REGISTRY_KEY) {
      testAction(ShareMySolutionsAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = false)
    }
  }

  fun `test ShareMySolutionsAction is not visible for non-marketplace course`() {
    setMarketplaceCourse(false)
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = false)
  }

  fun `test ShareMySolutionsAction is visible for marketplace course`() {
    setMarketplaceCourse(true)
    testAction(ShareMySolutionsAction.ACTION_ID, shouldBeEnabled = false, shouldBeVisible = true)
  }

  private fun setMarketplaceCourse(state: Boolean) {
    val course = project.course ?: error("Course is null")
    course.isMarketplace = state
  }
}