package com.jetbrains.edu.learning.courseFormat

import java.util.concurrent.atomic.AtomicReference

open class EduFile {
  var name: String = ""

  /**
   * The contents of this edu file encoded as a string.
   * If the file is textual, the contents are stored as is.
   * If the contents are binary, it is stored as a base64 encoded string.
   *
   * In the student mode, this field most of the time stores the initial contents of the file, i.e. the contents provided by the course author.
   * In the course creation mode, in tests, and sometimes in the student mode, this field stores the contents that are about to be written
   * to disk.
   *
   * It is preferable to use the [contents] property instead of the [text] property, because for the [text] property it is impossible to tell
   * whether the contents are binary or textual.
   * Currently, the [text] field delegates to the [contents] field.
   */
  var text: String
    @Deprecated("Use EduFile.contents to be sure, whether this data is textual ")
    get() = contents.textualRepresentation
    @Deprecated("Use EduFile.contents to specify explicitly is it binary or not")
    set(value) {
      contents = InMemoryUndeterminedContents(value)
    }

  /**
   * See the [text] field for the description.
   * The [contents] field, compared to the text field, also contains the information about whether the contents are binary or not.
   */
  private var _contents: AtomicReference<FileContents> = AtomicReference(UndeterminedContents.EMPTY)

  var contents: FileContents
    get() = _contents.get()
    set(value) = _contents.set(value)

  fun setContentsIfEquals(expectedValue: FileContents, newValue: FileContents): Boolean =
    _contents.compareAndSet(expectedValue, newValue)

  @Suppress("unused") // used for serialization
  val isBinary: Boolean?
    get() = when (contents) {
      is TextualContents -> false
      is BinaryContents -> true
      is UndeterminedContents -> null
    }


  var isTrackChanges: Boolean = true
  var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  open var isVisible: Boolean = false
  var isEditable: Boolean = true
  var isPropagatable: Boolean = true

  // Should be used only in student mode
  var isLearnerCreated: Boolean = false

  constructor()

  constructor(name: String, contents: FileContents) {
    this.name = name
    this.contents = contents
  }

  /**
   * The [contents] is interpreted as [InMemoryTextualContents]
   */
  constructor(name: String, contents: String) {
    this.name = name
    this.contents = InMemoryTextualContents(contents)
  }

  @Suppress("unused") // used for serialization
  fun getTextToSerialize(): String? {
    if (this !is TaskFile || !task.course.needWriteYamlText) {
      return null
    }

    // first, do not serialize binary contents
    if (contents is BinaryContents) return null
    if (contents is UndeterminedContents) {
      // fallback to the legacy file way to determine binarity
      val contentType = mimeFileType(name)
      if (contentType != null && isBinary(contentType)) return null
    }

    val text = contents.textualRepresentation

    if (exceedsBase64ContentLimit(text)) {
      LOG.warning(
        "Base64 encoding of `$name` file exceeds limit (${getBinaryFileLimit().toLong()}), " +
        "its content isn't serialized"
      )
      return null
    }

    return text
  }

  val pathInCourse: String
    get() {
      val pathPrefix = if (this is TaskFile) task.pathInCourse else ""
      val pathInCourse = "$pathPrefix/$name"

      return if (pathInCourse.startsWith('/')) {
        pathInCourse.substring(1)
      }
      else {
        pathInCourse
      }
    }

  companion object {
    val LOG = logger<EduFile>()
  }
}