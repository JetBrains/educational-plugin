package com.jetbrains.edu.learning.framework.impl

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

sealed interface UserChangesContents {
  val ordinal: Int
  fun write(out: DataOutput)

  companion object {
    fun fromFileContents(fileContents: FileContents): UserChangesContents = when (fileContents) {
      is BinaryContents -> BinaryContentChange(fileContents.bytes)
      is TextualContents -> TextualContentChange(fileContents.text)
      is UndeterminedContents -> UndeterminedContentChange(fileContents.textualRepresentation)
    }

    @Throws(IOException::class)
    fun write(fileContents: FileContents, out: DataOutput) {
      val contentsChange = fromFileContents(fileContents)
      out.writeInt(contentsChange.ordinal)
      contentsChange.write(out)
    }

    @Throws(IOException::class)
    fun read(input: DataInput): FileContents {
      return when (val ordinal = input.readInt()) {
        0 -> BinaryContentChange.read(input)
        1 -> TextualContentChange.read(input)
        2 -> UndeterminedContentChange.read(input)
        else -> error("Unexpected content type: $ordinal")
      }
    }
  }
}

class BinaryContentChange(override val bytes: ByteArray) : UserChangesContents, BinaryContents {
  override val ordinal: Int = 0

  override fun write(out: DataOutput) {
    out.writeInt(bytes.size)
    out.write(bytes, 0, bytes.size)
  }

  companion object {
    fun read(input: DataInput): BinaryContentChange {
      val length = input.readInt()
      val bytes = ByteArray(length)
      input.readFully(bytes, 0, length)
      return BinaryContentChange(bytes)
    }
  }
}

class TextualContentChange(override val text: String) : UserChangesContents, TextualContents {
  override val ordinal: Int = 1

  override fun write(out: DataOutput) {
    out.writeUTF(text)
  }

  companion object {
    fun read(input: DataInput): TextualContentChange {
      val text = input.readUTF()
      return TextualContentChange(text)
    }
  }
}

class UndeterminedContentChange(override val textualRepresentation: String) : UserChangesContents, UndeterminedContents {
  override val ordinal: Int = 2

  override fun write(out: DataOutput) {
    out.writeUTF(textualRepresentation)
  }

  companion object {
    fun read(input: DataInput): UndeterminedContentChange {
      val text = input.readUTF()
      return UndeterminedContentChange(text)
    }
  }
}