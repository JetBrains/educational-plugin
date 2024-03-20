package com.jetbrains.edu.learning.storage

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents

private val LOG: Logger = logger<FileContents>()

private fun warnContents(contents: FileContents, pathInStorage: String) {
  LOG.info("Contents of a file was accessed while the file was being persisted: $pathInStorage ${contents.javaClass}")
}

private class TextualContentsDiagnosticsWrapper(
  private val contents: TextualContents,
  private val pathInStorage: String
) : TextualContents {
  override val text: String
    get() {
      warnContents(contents, pathInStorage)
      return contents.text
    }
}

private class BinaryContentsDiagnosticsWrapper(
  private val contents: BinaryContents,
  private val pathInStorage: String
) : BinaryContents {
  override val bytes: ByteArray
    get() {
      warnContents(contents, pathInStorage)
      return contents.bytes
    }
}

private class UndeterminedContentsDiagnosticsWrapper(
  private val contents: UndeterminedContents,
  private val pathInStorage: String
) : UndeterminedContents {
  override val textualRepresentation: String
    get() {
      warnContents(contents, pathInStorage)
      return contents.textualRepresentation
    }
}

fun wrapWithDiagnostics(contents: FileContents, pathInStorage: String): FileContents = when (contents) {
  is TextualContents -> TextualContentsDiagnosticsWrapper(contents, pathInStorage)
  is BinaryContents -> BinaryContentsDiagnosticsWrapper(contents, pathInStorage)
  is UndeterminedContents -> UndeterminedContentsDiagnosticsWrapper(contents, pathInStorage)
}