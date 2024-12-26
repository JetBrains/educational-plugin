package com.jetbrains.edu.coursecreator.archive

import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.outputStream

interface CourseArchiveOutputProducer {
  fun createOutput(): OutputStream
}

/**
 * Provides [OutputStream] for given [location]
 */
fun CourseArchiveOutputProducer(location: Path): CourseArchiveOutputProducer = PathBasedCourseArchiveOutputProducer(location)

private class PathBasedCourseArchiveOutputProducer(private val location: Path) : CourseArchiveOutputProducer {
  override fun createOutput(): OutputStream = location.outputStream().buffered()
}
