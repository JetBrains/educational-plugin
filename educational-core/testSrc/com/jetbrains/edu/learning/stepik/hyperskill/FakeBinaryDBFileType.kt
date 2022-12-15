package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon

class FakeBinaryDBFileType : FileType {
  override fun getName(): String = "FakeDBFileType"

  @Suppress("HardCodedStringLiteral")
  override fun getDescription(): String = "DB binary file type for testing"

  override fun getDefaultExtension(): String = ".db"
  override fun getIcon(): Icon? = null
  override fun isBinary(): Boolean = true
}