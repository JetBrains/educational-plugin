package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.node.ObjectNode

interface JsonLocalCourseConverter {
  fun convert(localCourse: ObjectNode): ObjectNode
}
