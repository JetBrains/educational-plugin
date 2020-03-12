package com.jetbrains.edu.learning.courseFormat

// Order of items is important here because it uses for comparison
sealed class CourseCompatibility {
  object Compatible : CourseCompatibility()
  object IncompatibleVersion : CourseCompatibility()
  object Unsupported : CourseCompatibility()
}
