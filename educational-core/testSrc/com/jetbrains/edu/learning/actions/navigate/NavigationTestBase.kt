package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase

abstract class NavigationTestBase : EduTestCase() {
  protected val rootDir: VirtualFile get() = LightPlatformTestCase.getSourceRoot()
}