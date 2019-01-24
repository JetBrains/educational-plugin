package com.jetbrains.edu.learning.actions

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.io.storage.AbstractStorage
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl

abstract class NavigationTestBase : EduTestCase() {

  protected val rootDir: VirtualFile get() = LightPlatformTestCase.getSourceRoot()

  override fun tearDown() {
    AbstractStorage.deleteFiles(FrameworkLessonManagerImpl.constructStoragePath(project))
    super.tearDown()
  }
}
