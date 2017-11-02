package com.jetbrains.edu.learning

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.core.EduAnswerPlaceholderPainter

class RemovePlaceholderOnTypingHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project?, editor: Editor, file: PsiFile): TypedHandlerDelegate.Result {
        editor.markupModel.allHighlighters
                .filter { it.layer == EduAnswerPlaceholderPainter.PLACEHOLDERS_LAYER }
                .forEach { highlighter ->
                    if (editor.caretModel.offset in highlighter.startOffset..highlighter.endOffset) {
                        highlighter.textAttributes?.effectType = null
                    }
                }

        return Result.CONTINUE
    }
}
