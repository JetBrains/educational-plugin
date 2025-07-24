package com.jetbrains.edu.yaml

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.prevLeaf
import com.jetbrains.edu.learning.stepik.api.ADDITIONAL_FILES
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue

class AdditionalFileListEnterHandler : EnterHandlerDelegateAdapter() {
  
  /*override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): EnterHandlerDelegate.Result {
    if (file !is YAMLFile) return EnterHandlerDelegate.Result.Continue

    val document = editor.document
    PsiDocumentManager.getInstance(file.project).commitDocument(document)

    val offset = editor.caretModel.offset
    val lineNumber = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val lineText = document.getText(TextRange(lineStartOffset, offset)).trim()

//    if (lineText == "-") {
      val psiElement = file.findElementAt(lineStartOffset)
      var parent = psiElement?.parent
    print(parent)
*//*      while (parent != null && parent !is YAMLFile) {
        if (parent.text.trim().startsWith("additional_files:")) {
          EditorModificationUtil.insertStringAtCaret(editor, " name:")
          return EnterHandlerDelegate.Result.Default
        }
        parent = parent.parent
      }
//    }*//*
    return EnterHandlerDelegate.Result.Continue
  }*/

  /*override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): EnterHandlerDelegate.Result? {
    val result = super.postProcessEnter(file, editor, dataContext)

    if (result == EnterHandlerDelegate.Result.Stop) {
      val caretOffset = editor.caretModel.offset

      editor.document.insertString(caretOffset, "name: ")
      editor.caretModel.moveToOffset(caretOffset + "name: ".length)
    }

    return result
  }*/

  override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): EnterHandlerDelegate.Result? {
    PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)

    val offset = editor.caretModel.offset
    val element = file.findElementAt(offset) ?: return EnterHandlerDelegate.Result.Continue

    // Step 1: Try to find if caret is on/after the 'additional_files:' key
    val additionalFilesPsiElement = element
      .prevLeaf { it.parent is YAMLKeyValue }
      ?.parent as? YAMLKeyValue ?: return EnterHandlerDelegate.Result.Continue

    if (additionalFilesPsiElement.keyText != ADDITIONAL_FILES) return EnterHandlerDelegate.Result.Continue

    // Step 2: Ensure the value is a sequence or not present at all
//    val currentIndent = additionalFilesPsiElement.indent

//    val indentOptions = CodeStyle.getLanguageSettings(file, YAMLLanguage.INSTANCE).indentOptions ?: (CodeStyle.getSettings(file)?.indentOptions)
//    val indentSize = indentOptions?.INDENT_SIZE ?: 2
//    val insertIndent = (currentIndent ?: "") + " ".repeat(indentSize)

    // Step 3: Insert `- name: ` below
    val document = editor.document
    val insertText = "- name: "
//    val insertText = "$insertIndent- name: "

    val insertOffset = editor.caretModel.offset
    document.insertString(insertOffset, insertText)
    editor.caretModel.moveToOffset(insertOffset + insertText.length)

    return EnterHandlerDelegate.Result.DefaultForceIndent
  }

  // Helper: safely compute YAML indentation
  private val YAMLKeyValue.indent: String?
    get() {
      return text?.takeWhile { it == ' ' || it == '\t' }
    }
}