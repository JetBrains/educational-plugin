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

class TaskDescriptionStep : EduUiOnboardingStep {
    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData? {
        val taskToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            .getToolWindow("Task") ?: return null

        taskToolWindow.show()

        val component = taskToolWindow.component

        val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("task.description.step.text") }
            .withHeader(EduUiOnboardingBundle.message("task.description.step.header"))

        // Position the balloon at the bottom of the task tool window component
        val point = Point(component.width / 2, component.height - 20)
        val relativePoint = RelativePoint(component, point)
        return EduUiOnboardingStepData(builder, relativePoint, Balloon.Position.above)
    }

    override fun isAvailable(): Boolean = true
}