package com.jetbrains.edu.uiOnboarding

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test

class EduUiOnboardingServiceTest : LightPlatformTestCase() {
    @Test
    fun `test toad tour disabled by system property`() {
        try {
            System.setProperty(EduUiOnboardingService.SKIP_TOAD_TOUR_PROPERTY, "true")
            assertFalse("Tour should be disabled when property is set to true", 
                       EduUiOnboardingService.isToadTourEnabled())
        } finally {
            System.clearProperty(EduUiOnboardingService.SKIP_TOAD_TOUR_PROPERTY)
        }
    }

    @Test
    fun `test toad tour enabled by default`() {
        assertTrue("Tour should be enabled by default", EduUiOnboardingService.isToadTourEnabled())
    }

    @Test
    fun `test toad tour enabled with property set to false`() {
        try {
            System.setProperty(EduUiOnboardingService.SKIP_TOAD_TOUR_PROPERTY, "false")
            assertTrue("Tour should be enabled when property is set to false",
                      EduUiOnboardingService.isToadTourEnabled())
        } finally {
            System.clearProperty(EduUiOnboardingService.SKIP_TOAD_TOUR_PROPERTY)
        }
    }
}