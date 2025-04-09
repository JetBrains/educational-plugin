// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepData
import java.awt.Point

class CheckSolutionStep : EduUiOnboardingStep {
    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData? {
        val taskToolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow("Task") ?: return null

        taskToolWindow.show()

        val component = taskToolWindow.component

        val checkPanel = component.findComponentOfType(CheckPanel::class.java) ?: return null

        val leftActionsToolbar = findLeftActionsToolbar(checkPanel) ?: return null

        if (leftActionsToolbar !is java.awt.Container || leftActionsToolbar.componentCount == 0) return null
        val checkButtonWrapper = leftActionsToolbar.getComponent(0)

        val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("check.solution.step.text") }
            .withHeader(EduUiOnboardingBundle.message("check.solution.step.header"))

        val point = Point(checkButtonWrapper.width / 2, 0)
        val relativePoint = RelativePoint(checkButtonWrapper, point)
        return EduUiOnboardingStepData(builder, relativePoint, Balloon.Position.above)
    }

    private fun findLeftActionsToolbar(checkPanel: CheckPanel): java.awt.Component? {

        // Find the check actions panel (should be at index 0 or 1)
        val checkActionsPanel = findComponentWithBorderLayout(checkPanel) ?: return null

        if (checkActionsPanel !is java.awt.Container) return null

        // Look for a component with BoxLayout which would be the left actions toolbar
        for (i in 0 until checkActionsPanel.componentCount) {
            val component = checkActionsPanel.getComponent(i)
            if (component is javax.swing.JPanel && component.layout is javax.swing.BoxLayout) {
                return component
            }
        }

        return null
    }

    private fun findComponentWithBorderLayout(container: java.awt.Container): java.awt.Component? {
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)
            if (component is javax.swing.JPanel && component.layout is java.awt.BorderLayout) {
                return component
            }
        }
        return null
    }

    override fun isAvailable(): Boolean = true
}

private fun <T : Any> java.awt.Component.findComponentOfType(clazz: Class<T>): T? {
    if (clazz.isInstance(this)) return clazz.cast(this)
    if (this is java.awt.Container) {
        for (i in 0 until componentCount) {
            val component = getComponent(i)
            val result = component.findComponentOfType(clazz)
            if (result != null) return result
        }
    }
    return null
}

private fun java.awt.Component.findComponentByName(name: String): java.awt.Component? {
    if (this.name == name) return this
    if (this is java.awt.Container) {
        for (i in 0 until componentCount) {
            val component = getComponent(i)
            val result = component.findComponentByName(name)
            if (result != null) return result
        }
    }
    return null
}
