@file:JvmName("VirtualFileExt")

package com.jetbrains.edu.learning

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

val VirtualFile.document
  get() : Document = FileDocumentManager.getInstance().getDocument(this) ?: error("Cannot find document for a file: ${name}")
