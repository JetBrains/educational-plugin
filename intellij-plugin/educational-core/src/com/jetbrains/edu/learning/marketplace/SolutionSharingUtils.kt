package com.jetbrains.edu.learning.marketplace

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.jetbrains.annotations.NonNls

object SolutionSharingPromptCounter {

  private val promptCount: Int
    get() = propertiesComponent.getInt(PROMPT_COUNT, 0)

  private val propertiesComponent: PropertiesComponent
    get() = PropertiesComponent.getInstance()

  @NonNls
  private const val PROMPT_COUNT = "com.jetbrains.edu.Marketplace.Solution.Sharing.Prompt.Count"

  @NonNls
  private const val PROMPT_SHOWN_AT = "com.jetbrains.edu.Marketplace.Solution.Sharing.Prompt.Shown.At"

  @NonNls
  private const val REGISTRY_KEY = "edu.marketplace.solutions.sharing.prompt.default.delay"

  private val DELAY: Long
    get() = Registry.intValue(REGISTRY_KEY).toLong()

  fun shouldPrompt(course: EduCourse): Boolean {
    if (!course.areCommunitySolutionsSupported()) {
      return false
    }
    if (UserAgreementSettings.getInstance().solutionSharing) {
      return false
    }

    return when (promptCount) {
      0 -> true
      1 -> isDelayPassed()
      else -> false
    }
  }

  fun update() {
    propertiesComponent.setValue(PROMPT_SHOWN_AT, System.currentTimeMillis().toString())
    when (promptCount) {
      0 -> propertiesComponent.setValue(PROMPT_COUNT, "1")
      1 -> propertiesComponent.setValue(PROMPT_COUNT, "-1")
      else -> return
    }
  }

  private fun isDelayPassed(): Boolean {
    val timestamp = propertiesComponent.getLong(PROMPT_SHOWN_AT, 0)
    val current = System.currentTimeMillis()
    return current - timestamp > DELAY
  }
}

fun EduCourse.areCommunitySolutionsSupported() : Boolean = isMarketplaceRemote && isStudy && !isFromCourseStorage()

