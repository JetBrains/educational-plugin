package com.jetbrains.edu.learning.courseFormat

import java.util.*

/**
 * A representation of some file contents, together with the information whether these contents
 * should be interpreted as binary or as textual.
 * There are two reasons to distinguish between two types of contents:
 *  1. The `course.json` format can store only textual information, so binary files should be encoded before they go into it.
 *  2. Text files preserve their contents independently on the encodings of a teacher's or a learner's computer.
 *
 *  This class may represent contents stored in different places, for example, the contents may be stored in memory, come directly from disk,
 *  or may be stored inside a database.
 */
sealed interface FileContents {
  /**
   * This is a base64 encoding of a binary contents, a plain text for textual contents, or one of these variants if the type is unknown
   */
  val textualRepresentation: String
}

/**
 * The FileContents that is either binary or textual.
 * A counterpart of the UndeterminedFileContents interface.
 */
sealed interface DeterminedContents : FileContents

interface TextualContents : DeterminedContents {

  val text: String

  override val textualRepresentation: String
    get() = text

  companion object {
    val EMPTY = object : TextualContents {
      override val text
        get() = ""
    }
  }
}

interface BinaryContents : DeterminedContents {

  val bytes: ByteArray

  override val textualRepresentation: String
    get() = Base64.getEncoder().encodeToString(bytes)

  companion object {
    private val EMPTY_BYTE_ARRAY = byteArrayOf()
    val EMPTY = object : BinaryContents {
      override val bytes: ByteArray
        get() = EMPTY_BYTE_ARRAY
    }
  }
}

/**
 * Represents file contents, for which we know only its textual representation and don't know whether it is
 * a text or a base64 encoding of a byte array.
 * When we determine this somehow externally, for example, by the file name,
 * we retrieve a value of either a [text] field, or a [bytes] field.
 * If we determine the actual type incorrectly, we get broken data, or even get an exception, if it is impossible
 * to parse base64 encoding of a [textualRepresentation].
 */
interface UndeterminedContents : FileContents {

  override val textualRepresentation: String

  val text: String
    get() = textualRepresentation

  val bytes: ByteArray
    get() = Base64.getDecoder().decode(textualRepresentation)

  companion object {
    val EMPTY = object : UndeterminedContents {
      override val textualRepresentation: String = ""
    }
  }
}

/**
 * Represents a binary FileContents stored in memory.
 * These contents are not persistent.
 */
class InMemoryBinaryContents(override val bytes: ByteArray): BinaryContents

/**
 * Represents a textual FileContents stored in memory.
 * These contents are not persistent.
 */
class InMemoryTextualContents(override val text: String) : TextualContents

/**
 * Represents a FileContents of an unknown type stored in memory.
 * These contents are not persistent.
 */
class InMemoryUndeterminedContents(override val textualRepresentation: String) : UndeterminedContents