package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

abstract class EduFileEditorTestFixtureBase(protected val fixture: CodeInsightTestFixture) : BaseFixture() {

  private lateinit var manager: FileEditorManagerImpl

  override fun setUp() {
    super.setUp()
    manager = createFileEditorManager(CoroutineScope(EmptyCoroutineContext))

    // Copied from TestEditorManagerImpl's constructor
    manager.registerExtraEditorDataProvider(TextEditorPsiDataProvider(), null)
    replaceManager(manager)
  }

  protected abstract fun replaceManager(manager: FileEditorManager)

  override fun tearDown() {
    try {
      manager.closeAllFiles()
    }
    finally {
      super.tearDown()
    }
  }

  protected abstract fun createFileEditorManager(scope: CoroutineScope): FileEditorManagerImpl
}