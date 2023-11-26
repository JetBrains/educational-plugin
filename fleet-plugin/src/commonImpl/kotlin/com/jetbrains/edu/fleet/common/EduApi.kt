package com.jetbrains.edu.fleet.common

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.rhizomedb.Entity

interface CourseEntity: Entity {
  var course: Course
}