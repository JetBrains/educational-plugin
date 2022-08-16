package com.jetbrains.edu.learning.courseFormat

open class EduFile {
  var name: String = ""
  var text: String = ""

  var isTrackChanges: Boolean = true
  var isHighlightErrors: Boolean = false
  var isVisible: Boolean = true
  var isEditable: Boolean = true

  // Should be used only in student mode
  var isLearnerCreated: Boolean = false

  constructor()
  constructor(name: String, text: String) {
    this.name = name
    this.text = text
  }

  constructor(name: String, text: String, isVisible: Boolean) : this(name, text) {
    this.isVisible = isVisible
  }

  constructor(name: String, text: String, isVisible: Boolean, isLearnerCreated: Boolean) : this(name, text, isVisible) {
    this.isLearnerCreated = isLearnerCreated
  }

  @Suppress("unused") // used for serialization
  fun getTextToSerialize(): String? {
    if (exceedsBase64ContentLimit(text)) {
      LOG.warn("Base64 encoding of `$name` file exceeds limit (${getBinaryFileLimit().toLong()}), " +
               "its content isn't serialized")
      return null
    }
    val contentType = mimeFileType(name) ?: return text
    return if (isBinary(contentType)) null else text
  }

  companion object {
    val LOG = logger<EduFile>()
  }
}