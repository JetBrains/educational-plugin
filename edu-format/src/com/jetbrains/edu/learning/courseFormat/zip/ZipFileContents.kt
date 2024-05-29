package com.jetbrains.edu.learning.courseFormat.zip

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import java.util.zip.ZipFile
import kotlin.text.Charsets.UTF_8

class ZipBinaryContents(private val zipPath: String, private val eduFile: EduFile) : BinaryContents {
  override val bytes: ByteArray
    get() = ZipFile(zipPath).use { zip ->
      val path = eduFile.pathInCourse
      val entry = zip.getEntry(path)
      zip.getInputStream(entry).readAllBytes()
    }
}

class ZipTextualContents(private val zipPath: String, private val eduFile: EduFile) : TextualContents {
  override val text: String
    get() = ZipFile(zipPath).use { zip ->
      val path = eduFile.pathInCourse
      val entry = zip.getEntry(path)
      val bytes = zip.getInputStream(entry).readAllBytes()

      String(bytes, UTF_8)
    }
}