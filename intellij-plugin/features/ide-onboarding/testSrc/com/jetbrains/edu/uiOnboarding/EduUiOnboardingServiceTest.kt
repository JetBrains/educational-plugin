package com.jetbrains.edu.uiOnboarding

import com.intellij.testFramework.LightPlatformTestCase

class EduUiOnboardingServiceTest : LightPlatformTestCase() {
    fun `test toad tour enabled by default`() {
        assertFalse("Tour should be enabled by default", skipToadTourOnProjectOpen())
    }
}