package com.jetbrains.edu.cognifire.ui

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.actionSystem.ReadonlyFragmentModificationHandler
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle

class ProdeReadonlyFragmentHandler(private val editor: Editor, private val isPromptChange: Boolean) : ReadonlyFragmentModificationHandler {
  override fun handle(e: ReadOnlyFragmentModificationException) {
    if (editor.isDisposed) return
    val message = if (isPromptChange)
      EduCognifireBundle.message("notification.text.error.prompt.changes.not.allowed") else
        EduCognifireBundle.message("notification.text.error.code.changes.not.allowed")
    HintManager.getInstance().showErrorHint(editor, message)
  }
}
