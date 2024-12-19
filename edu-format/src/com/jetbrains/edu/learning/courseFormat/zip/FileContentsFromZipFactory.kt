package com.jetbrains.edu.learning.courseFormat.zip

import com.jetbrains.edu.learning.cipher.Cipher
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.FileContentsFactory

class FileContentsFromZipFactory(private val zipPath: String, private val cipher: Cipher) : FileContentsFactory {
  override fun createBinaryContents(file: EduFile) = ZipBinaryContents(zipPath, file, cipher)
  override fun createTextualContents(file: EduFile) = ZipTextualContents(zipPath, file, cipher)
}