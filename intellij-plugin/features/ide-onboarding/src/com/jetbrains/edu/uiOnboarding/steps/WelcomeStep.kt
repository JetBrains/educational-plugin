// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepData
import java.awt.Point

class WelcomeStep : EduUiOnboardingStep {
    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData? {
        val frame = WindowManager.getInstance().getIdeFrame(project) ?: return null
        val component = frame.component

        val zhabaIcon = IconLoader.getIcon("images/zhaba-welcome.png", this::class.java.classLoader)
        val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("welcome.step.text") }
            .withImage(zhabaIcon)
            .withHeader(EduUiOnboardingBundle.message("welcome.step.header"))

        // Position the balloon in the center of the screen
        val point = Point(component.width / 2, component.height / 2)
        val relativePoint = RelativePoint(component, point)
        return EduUiOnboardingStepData(builder, relativePoint, null) // null position means center
    }

    override fun isAvailable(): Boolean = true
}
