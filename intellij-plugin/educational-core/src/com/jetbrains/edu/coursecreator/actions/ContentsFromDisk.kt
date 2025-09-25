package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readBytes
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

class BinaryContentsFromDisk(val file: VirtualFile) : BinaryContents {
  override val bytes: ByteArray
    get() = runReadAction {
      try {
        file.contentsToByteArray()
      }
      catch (_: FileTooBigException) {
        throw HugeBinaryFileException(file.path, file.length, FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong())
      }
    }
}

class TextualContentsFromDisk(val file: VirtualFile) : TextualContents {
  override val text: String
    get() {
      val bytes = runReadAction {
        file.readBytes()
      }
      val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)

      try {
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
      }
      catch (e : IOException) {
        if (e is MalformedInputException || e is UnmappableCharacterException || e is CharacterCodingException) {
          throw TextualContentsDecodingException(e)
        }
        else throw e
      }
    }
}

class TextualContentsDecodingException(cause: Throwable) : Exception(
  "Failed to decode file contents as a UTF-8 text",
  cause
)