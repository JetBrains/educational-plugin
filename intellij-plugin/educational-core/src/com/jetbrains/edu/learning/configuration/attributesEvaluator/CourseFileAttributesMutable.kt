package com.jetbrains.edu.learning.configuration.attributesEvaluator

import com.jetbrains.edu.learning.configuration.CourseFileAttributes

internal class CourseFileAttributesMutable {
  fun toImmutable(): CourseFileAttributes =
    CourseFileAttributes(excludedFromArchive)

  var excludedFromArchive: Boolean = false
}