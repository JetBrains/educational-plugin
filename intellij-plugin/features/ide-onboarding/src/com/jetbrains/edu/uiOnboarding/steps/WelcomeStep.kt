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

class WelcomeStep : EduUiOnboardingStep {
    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData? {
        val projectViewToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                                        .getToolWindow("Project") ?: return null
        projectViewToolWindow.show()

        val component = projectViewToolWindow.component

        val zhabaComponent = createZhaba(project, disposable)
        val zhabaDimension = zhabaComponent.dimension

        // Position of the zhaba on the project view component
        val zhabaPoint = Point(
            (component.width - zhabaDimension.width) / 2,
            component.height - zhabaDimension.height
        )
        zhabaComponent.zhabaPoint = RelativePoint(component, zhabaPoint)

        // Position the balloon at the bottom of the project view component
        val point = Point(component.width / 2, component.height - zhabaDimension.height - 10)
        val relativePoint = RelativePoint(component, point)

        val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("welcome.step.text") }
            .withHeader(EduUiOnboardingBundle.message("welcome.step.header"))

        return EduUiOnboardingStepData(builder, relativePoint, Balloon.Position.above, zhabaComponent)
    }

    override fun isAvailable(): Boolean = true
    override val zhabaID: String = "zhaba-welcome"
}
