package com.jetbrains.edu.learning.courseFormat.zip

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.json.encrypt.Cipher
import com.jetbrains.edu.learning.json.pathInArchive
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.ZipFile

class ZipBinaryContents(zipPath: String, eduFile: EduFile, cipher: Cipher) : ZipContents(
  zipPath,
  eduFile,
  cipher
), BinaryContents {
  override val bytes: ByteArray
    get() = loadBytes()
}

class ZipTextualContents(zipPath: String, eduFile: EduFile, cipher: Cipher) : ZipContents(
  zipPath,
  eduFile,
  cipher
), TextualContents {
  override val text: String
    get() = String(loadBytes(), UTF_8)
}

sealed class ZipContents(private val zipPath: String, private val eduFile: EduFile, private val cipher: Cipher) {
  protected fun loadBytes(): ByteArray {
    try {
      val encryptedBytes = ZipFile(zipPath).use { zip ->
        val path = eduFile.pathInArchive
        val entry = zip.getEntry(path)
        zip.getInputStream(entry).readAllBytes()
      }

      return cipher.decrypt(encryptedBytes)
    }
    catch (e: Exception) {
      logger<ZipContents>().severe("Unable to read edu file ${eduFile.pathInArchive} contents from zip")
      return EMPTY_BYTES
    }
  }

  companion object {
    private val EMPTY_BYTES = byteArrayOf()
  }
}