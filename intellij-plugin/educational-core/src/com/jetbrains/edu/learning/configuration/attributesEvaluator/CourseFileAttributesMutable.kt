package com.jetbrains.edu.learning.configuration.attributesEvaluator

import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseFileAttributes
import com.jetbrains.edu.learning.configuration.CourseViewVisibility

internal class CourseFileAttributesMutable {
  fun toImmutable(): CourseFileAttributes =
    CourseFileAttributes(legacyExcludedFromArchive, inclusionPolicy, visibility)

  var legacyExcludedFromArchive: Boolean = false
  var inclusionPolicy: ArchiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT
  var visibility: CourseViewVisibility = CourseViewVisibility.AUTHOR_DECISION
}