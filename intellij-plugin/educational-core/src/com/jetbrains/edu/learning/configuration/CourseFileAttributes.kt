package com.jetbrains.edu.learning.configuration

/**
 * Whether a file should be added to the course archive.
 * For some files, a plugin can confidently say whether they should go into an archive or not;
 * for other files users can decide that themselves.
 * These values are evaluated for each file from the list of additional files provided by a user.
 * If there is an inconsistency, for example, some important file is absent in the list, or a list contains a wrong file, the warnings
 * and errors should be displayed.
 */
enum class ArchiveInclusionPolicy {
  /**
   * Including the file in the course archive may break the course.
   * Such files must not go to the archive, even if a user has added the files manually.
   */
  MUST_EXCLUDE,

  /**
   * The author decides whether the file is needed in the archive; no warnings and errors are supposed.
   */
  AUTHOR_DECISION,

  /**
   * These files are included by default because they are most likely important for the course.
   * For example, files that describe the project (`package.json`, `build.sbt`, etc.) are important, and they will almost
   * always be regenerated on the learner side.
   * But it is better to anyway put them to the archive not to rely on the regeneration.
   * If the file with the [INCLUDED_BY_DEFAULT] value is not listed as additional, it will not be added to the archive.
   */
  INCLUDED_BY_DEFAULT
}

/**
 * Specifies the visibility of the file in the course view.
 *
 * The visibility of most files in the Course View is determined by the author, they
 * specify visibility for files manually.
 * But some files must be forcibly hidden or must be forcibly shown for any course.
 * For example, the build output should never be visible.
 * The rules forcing the visibility of files are stated with this enum.
 */
enum class CourseViewVisibility {
  /**
   * Invisible for both learners and authors
   */
  INVISIBLE_FOR_ALL,

  /**
   * No special visibility rules for the file.
   * Its visibility is determined by an author.
   */
  AUTHOR_DECISION,

  /**
   * Forcibly visible for students.
   * For directories: visible with all their contents.
   */
  VISIBLE_FOR_STUDENT
}

data class CourseFileAttributes(
  /**
   * Whether a file should be excluded from a course archive automatically.
   * This value preserves the old behavior of the plugin, when additional files were not explicitly listed in the `course-info.yaml`.
   * That time all files from the disk were put to archive, except the files, for which `EduConfigurator.excludeFromArchive()` was `true`.
   * The value is used when the course is migrated from a version without an implicit list of additional files (YAML before v2).
   * Other usages of this field are considered legacy and should be eliminated with time.
   */
  val excludedFromArchive : Boolean,

  /**
   * See [ArchiveInclusionPolicy]. The default value is [ArchiveInclusionPolicy.AUTHOR_DECISION]
   */
  val archiveInclusionPolicy: ArchiveInclusionPolicy,

  /**
   * See [CourseViewVisibility]. The default value is [CourseViewVisibility.AUTHOR_DECISION]
   */
  val visibility: CourseViewVisibility
)
