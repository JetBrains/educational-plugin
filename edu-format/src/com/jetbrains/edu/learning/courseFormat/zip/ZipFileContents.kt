package com.jetbrains.edu.learning.courseFormat.zip

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.json.encrypt.AES256
import com.jetbrains.edu.learning.json.pathInArchive
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.ZipFile

class ZipBinaryContents(zipPath: String, eduFile: EduFile, aesKey: String?) : ZipContents(
  zipPath,
  eduFile,
  aesKey
), BinaryContents {
  override val bytes: ByteArray
    get() = loadBytes()
}

class ZipTextualContents(zipPath: String, eduFile: EduFile, aesKey: String?) : ZipContents(
  zipPath,
  eduFile,
  aesKey
), TextualContents {
  override val text: String
    get() = String(loadBytes(), UTF_8)
}

sealed class ZipContents(private val zipPath: String, private val eduFile: EduFile, private val aesKey: String?) {
  protected fun loadBytes(): ByteArray {
    val encryptedBytes = ZipFile(zipPath).use { zip ->
      val path = eduFile.pathInArchive
      val entry = zip.getEntry(path)
      zip.getInputStream(entry).readAllBytes()
    }

    return if (aesKey != null) {
      AES256.decrypt(encryptedBytes, aesKey)
    }
    else {
      encryptedBytes
    }
  }
}