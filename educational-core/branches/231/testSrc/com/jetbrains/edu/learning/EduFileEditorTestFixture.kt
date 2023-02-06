package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

class EduFileEditorTestFixture(fixture: CodeInsightTestFixture) : EduFileEditorTestFixtureBase(fixture) {
  override fun createFileEditorManager(): FileEditorManagerImpl = FileEditorManagerImpl(fixture.project, fixture.project.coroutineScope)
}
