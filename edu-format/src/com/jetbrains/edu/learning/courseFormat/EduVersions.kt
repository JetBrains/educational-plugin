@file:JvmName("EduVersions")

package com.jetbrains.edu.learning.courseFormat

// If you change version of any format, add point about it in `documentation/Versions.md`
const val JSON_FORMAT_VERSION: Int = 18
// We need the two versions here because the 19's version is generated with the feature flag COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON
const val JSON_FORMAT_VERSION_WITH_FILES_OUTSIDE: Int = 19

/**
 * Since that version we deprecated [com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE] and started to use
 * new [com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_ID] and
 * [com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION] properties
 *
 * @see [com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin.programmingLanguage]
 * @see [com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin.languageId]
 * @see [com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin.languageVersion]
 */
const val JSON_FORMAT_VERSION_WITH_NEW_LANGUAGE_VERSION: Int = 16