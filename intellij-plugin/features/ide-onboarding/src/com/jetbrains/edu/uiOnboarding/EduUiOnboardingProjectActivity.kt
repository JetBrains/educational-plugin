package com.jetbrains.edu.uiOnboarding

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject

class EduUiOnboardingProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    if (!project.isStudentProject()) return

    // Check if the tour should be skipped via system property
    if (System.getProperty("edu.ui.onboarding.skip")?.toBoolean() == true) {
      // Mark the tour as shown without actually showing it
      PropertiesComponent.getInstance().setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)
      return
    }

    val propertiesComponent = PropertiesComponent.getInstance()

    val shown = propertiesComponent.getBoolean(EDU_UI_ONBOARDING_TOUR_SHOWN)
    if (shown) return
    propertiesComponent.setValue(EDU_UI_ONBOARDING_TOUR_SHOWN, true)

    EduUiOnboardingService.getInstance(project).startOnboarding()
  }
}

private const val EDU_UI_ONBOARDING_TOUR_SHOWN = "edu.ui.onboarding.tour.shown"
