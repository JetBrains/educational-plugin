// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepData
import java.awt.Point

class CodeEditorStep : EduUiOnboardingStep {
    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null

        val component = editor.component

        val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("code.editor.step.text") }
            .withHeader(EduUiOnboardingBundle.message("code.editor.step.header"))

        val zhabaComponent = createZhaba(project, disposable)
        val dimension = zhabaComponent.dimension

        val zhabaPoint = Point(component.width / 2, 0)
        zhabaComponent.zhabaPoint = RelativePoint(component, zhabaPoint)

        // Position the balloon below the zhaba
        val point = Point(component.width / 2 + dimension.width - 30, dimension.height + 10)
        val relativePoint = RelativePoint(component, point)
        return EduUiOnboardingStepData(builder, relativePoint, Balloon.Position.below, zhabaComponent)
    }

    override fun isAvailable(): Boolean = true
    override val zhabaID: String = "zhaba-editor"
}