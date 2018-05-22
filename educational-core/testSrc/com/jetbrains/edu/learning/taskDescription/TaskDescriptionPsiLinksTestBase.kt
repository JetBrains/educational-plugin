package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow
import junit.framework.TestCase

abstract class TaskDescriptionPsiLinksTestBase : EduTestCase() {

  abstract val fileType: FileType

  protected fun doTest(linkText: String, expectedText: String, fileTreeBlock: FileTreeBuilder.() -> Unit) {
    fileTree(fileTreeBlock).create(LightPlatformTestCase.getSourceRoot())
    TaskDescriptionToolWindow.navigateToPsiElement(project, "${TaskDescriptionToolWindow.PSI_ELEMENT_PROTOCOL}$linkText")
    UIUtil.dispatchAllInvocationEvents()
    val openedEditor = EditorFactory.getInstance().allEditors.single()

    myFixture.configureByText(fileType, expectedText.trimIndent())
    TestCase.assertEquals(openedEditor.document.text, myFixture.editor.document.text)
    TestCase.assertEquals(openedEditor.caretModel.logicalPosition, myFixture.editor.caretModel.logicalPosition)
  }
}
