package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.vfs.VirtualFile

fun FileEditorManagerEx.openFiles(outputFile: VirtualFile, inputFile: VirtualFile, windows: Array<EditorWindow>) {
  openFile(outputFile, windows[1])
  openFile(inputFile, windows[0])
}