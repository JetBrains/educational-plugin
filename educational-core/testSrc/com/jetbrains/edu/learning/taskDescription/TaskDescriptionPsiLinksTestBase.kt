package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionLinkProtocol
import com.jetbrains.edu.learning.taskDescription.ui.ToolWindowLinkHandler

abstract class TaskDescriptionPsiLinksTestBase : EduTestCase() {

  abstract val fileType: FileType

  protected fun doTest(linkText: String, expectedText: String, fileTreeBlock: FileTreeBuilder.() -> Unit) {
    fileTree(fileTreeBlock).create(LightPlatformTestCase.getSourceRoot())
    ToolWindowLinkHandler(project).process("${TaskDescriptionLinkProtocol.PSI_ELEMENT}$linkText")
    UIUtil.dispatchAllInvocationEvents()
    val openedEditor = EditorFactory.getInstance().allEditors.single()

    myFixture.configureByText(fileType, expectedText.trimIndent())
    assertEquals(openedEditor.document.text, myFixture.editor.document.text)
    assertEquals(openedEditor.caretModel.logicalPosition, myFixture.editor.caretModel.logicalPosition)
  }
}
