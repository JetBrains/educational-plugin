package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import kotlinx.coroutines.CoroutineScope

class EduFileEditorTestFixture(fixture: CodeInsightTestFixture) : EduFileEditorTestFixtureBase(fixture) {
  override fun createFileEditorManager(scope: CoroutineScope): FileEditorManagerImpl? = null

  override fun replaceManager(manager: FileEditorManager?) {}
}
