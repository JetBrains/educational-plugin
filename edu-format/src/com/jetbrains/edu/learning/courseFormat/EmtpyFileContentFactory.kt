package com.jetbrains.edu.learning.courseFormat

object EmtpyFileContentFactory : FileContentsFactory {
  override fun createBinaryContents(file: EduFile) = BinaryContents.EMPTY
  override fun createTextualContents(file: EduFile) = TextualContents.EMPTY
}