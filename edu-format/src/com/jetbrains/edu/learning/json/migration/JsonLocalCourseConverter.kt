package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode

interface JsonLocalCourseConverter {
  fun convert(localCourse: ObjectNode): ObjectNode
}
