package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.CoroutineScope

class EduFileEditorTestFixture(fixture: CodeInsightTestFixture) : EduFileEditorTestFixtureBase(fixture) {
  override fun createFileEditorManager(scope: CoroutineScope): FileEditorManagerImpl = FileEditorManagerImpl(fixture.project, scope)

  // BACKCOMPACT: 2022.3 Inline
  override fun replaceManager(manager: FileEditorManager) {
    fixture.project.replaceService(FileEditorManager::class.java, manager, testRootDisposable)
  }
}
