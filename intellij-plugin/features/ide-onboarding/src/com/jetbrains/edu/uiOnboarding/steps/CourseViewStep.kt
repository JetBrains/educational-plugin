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

        val zhabaComponent = createZhaba(project, disposable)
        val dimension = zhabaComponent.dimension

        val zhabaPoint = Point(
            (component.width - dimension.width) / 2,
            component.height - dimension.height
        )
        zhabaComponent.zhabaPoint = RelativePoint(component, zhabaPoint)

        // Position the balloon a bit to the right from the middle of the project view
        val point = Point(component.width / 2 + dimension.width / 2 - 10, component.height - dimension.height / 2 + 20)
        val relativePoint = RelativePoint(component, point)

        return EduUiOnboardingStepData(builder, relativePoint, Balloon.Position.above, zhabaComponent)
    }

    override fun isAvailable(): Boolean = true
    override val zhabaID: String = "zhaba-project"
}
