// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepData
import java.awt.Point

class CourseViewStep : EduUiOnboardingStep {
    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData? {
        val projectViewToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            .getToolWindow("Project") ?: return null

        projectViewToolWindow.show()

        val component = projectViewToolWindow.component

        val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("course.view.step.text") }
            .withHeader(EduUiOnboardingBundle.message("course.view.step.header"))

        // Position the balloon at the bottom of the project view component
        val point = Point(component.width / 2, component.height - 20)
        val relativePoint = RelativePoint(component, point)
        return EduUiOnboardingStepData(builder, relativePoint, Balloon.Position.above)
    }

    override fun isAvailable(): Boolean = true
}
