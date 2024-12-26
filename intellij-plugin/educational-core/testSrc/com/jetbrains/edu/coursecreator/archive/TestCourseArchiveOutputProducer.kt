package com.jetbrains.edu.coursecreator.archive

import java.io.ByteArrayOutputStream
import java.io.OutputStream

class TestCourseArchiveOutputProducer : CourseArchiveOutputProducer {

  private var out: ByteArrayOutputStream? = null

  override fun createOutput(): OutputStream {
    val output = ByteArrayOutputStream()
    out = output
    return output
  }

  fun data(): ByteArray {
    val out = requireNotNull(out) { "`createOutput` wasn't called" }
    return out.toByteArray()
  }
}
