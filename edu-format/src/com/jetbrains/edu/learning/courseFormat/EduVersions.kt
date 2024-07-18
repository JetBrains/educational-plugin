@file:JvmName("EduVersions")

package com.jetbrains.edu.learning.courseFormat

// If you change version of any format, add point about it in `documentation/Versions.md`
const val JSON_FORMAT_VERSION: Int = 18
// We need the two versions here because the 19's version is generated with the feature flag COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON
const val JSON_FORMAT_VERSION_WITH_FILES_OUTSIDE: Int = 19
