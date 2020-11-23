package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import com.intellij.testFramework.registerComponentInstance

class EduFileEditorTestFixture(private val fixture: CodeInsightTestFixture) : BaseFixture() {
  private lateinit var myManager: FileEditorManagerImpl

  override fun setUp() {
    super.setUp()

    myManager = FileEditorManagerImpl(fixture.project)

    // Copied from TestEditorManagerImpl's constructor
    myManager.registerExtraEditorDataProvider(TextEditorPsiDataProvider(), null)
    fixture.project.registerComponentInstance(FileEditorManager::class.java, myManager, testRootDisposable)
  }

  override fun tearDown() {
    try {
      myManager.closeAllFiles()
    }
    finally {
      super.tearDown()
    }
  }
}