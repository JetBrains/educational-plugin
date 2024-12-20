package com.jetbrains.edu.learning.courseFormat

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level

private val mimeBinaryTypes = listOf(
  "image",
  "audio",
  "video",
  "application",
  "font"
).map { "$it/" }

fun isBinary(contentType: String): Boolean {
  return mimeBinaryTypes.any { contentType.startsWith(it) }
}

/**
 * Note: this method works as expected only for paths on local file system as it uses Path under the hood
 * So it doesn't work properly in tests where in-memory file system is used
 */
fun mimeFileType(path: String): String? {
  return try {
    Files.probeContentType(Paths.get(path))
  }
  catch (e: IOException) {
    LOG.log(Level.SEVERE, "Failed to determine file mimetype", e)
    null
  }
}

fun exceedsBase64ContentLimit(base64text: String): Boolean {
  return base64text.toByteArray(StandardCharsets.UTF_16).size > getBinaryFileLimit()
}

fun getBinaryFileLimit(): Int {
  return 1024 * 1024
}

fun getExtension(fileName: String): String {
  val index = fileName.lastIndexOf('.')
  return if (index < 0) "" else fileName.substring(index + 1)
}

private val LOG = logger<StudyItem>()
