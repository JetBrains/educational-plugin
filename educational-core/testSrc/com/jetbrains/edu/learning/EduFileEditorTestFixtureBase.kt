package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import com.intellij.testFramework.registerComponentInstance

abstract class EduFileEditorTestFixtureBase(protected val fixture: CodeInsightTestFixture) : BaseFixture() {
  private lateinit var manager: FileEditorManagerImpl

  override fun setUp() {
    super.setUp()

    manager = createFileEditorManager()

    // Copied from TestEditorManagerImpl's constructor
    manager.registerExtraEditorDataProvider(TextEditorPsiDataProvider(), null)
    fixture.project.registerComponentInstance(FileEditorManager::class.java, manager, testRootDisposable)
  }

  override fun tearDown() {
    try {
      manager.closeAllFiles()
    }
    finally {
      super.tearDown()
    }
  }

  protected abstract fun createFileEditorManager(): FileEditorManagerImpl
}