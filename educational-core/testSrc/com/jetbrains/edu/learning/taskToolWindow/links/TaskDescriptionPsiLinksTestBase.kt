package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.fileTree

abstract class TaskDescriptionPsiLinksTestBase : TaskDescriptionLinksTestBase() {

  abstract val fileType: FileType

  protected fun doTest(linkText: String, expectedText: String, fileTreeBlock: FileTreeBuilder.() -> Unit) {
    fileTree(fileTreeBlock).create(LightPlatformTestCase.getSourceRoot())
    openLink(linkText)
    val openedEditor = EditorFactory.getInstance().allEditors.single()

    myFixture.configureByText(fileType, expectedText.trimIndent())
    assertEquals(openedEditor.document.text, myFixture.editor.document.text)
    assertEquals(openedEditor.caretModel.logicalPosition, myFixture.editor.caretModel.logicalPosition)
  }
}
