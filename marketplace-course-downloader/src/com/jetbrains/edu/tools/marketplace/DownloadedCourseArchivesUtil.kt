package com.jetbrains.edu.tools.marketplace

import com.jetbrains.edu.learning.cipher.Cipher
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.FileContentsFactory
import com.jetbrains.edu.learning.courseFormat.zip.FileContentsFromZipFactory
import com.jetbrains.edu.learning.json.readCourseJson
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.streams.asSequence

data class ArchiveIterationStats(
  val foundZipFiles: Int,
  val convertedToEduCourses: Int,
  val skippedNonEduOrBrokenArchives: Int
)

class DownloadedCourseArchivesUtil(
  private val courseCipher: Cipher = Cipher()
) {
  fun loadEduCoursesFromDownloadedZips(downloadsDir: Path): List<EduCourse> {
    val courses = mutableListOf<EduCourse>()
    forEachEduCourseInDownloadedZips(downloadsDir) { _, course ->
      courses += course
    }
    return courses
  }

  fun forEachEduCourseInDownloadedZips(
    downloadsDir: Path,
    onEduCourse: (zipPath: Path, course: EduCourse) -> Unit
  ): ArchiveIterationStats {
    if (!Files.exists(downloadsDir)) {
      return ArchiveIterationStats(foundZipFiles = 0, convertedToEduCourses = 0, skippedNonEduOrBrokenArchives = 0)
    }

    var foundZipFiles = 0
    var converted = 0
    var skipped = 0

    Files.walk(downloadsDir).use { paths ->
      paths.asSequence()
        .filter { Files.isRegularFile(it) && it.name.endsWith(".zip", ignoreCase = true) }
        .forEach { zipPath ->
          foundZipFiles++
          val course = readCourseFromArchive(zipPath)
          val eduCourse = course as? EduCourse
          if (eduCourse == null) {
            skipped++
            return@forEach
          }

          converted++
          onEduCourse(zipPath, eduCourse)
        }
    }

    return ArchiveIterationStats(
      foundZipFiles = foundZipFiles,
      convertedToEduCourses = converted,
      skippedNonEduOrBrokenArchives = skipped
    )
  }

  private fun readCourseFromArchive(zipPath: Path): Course? {
    return try {
      ZipFile(zipPath.toFile()).use { zipFile ->
        val courseEntry = zipFile.getEntry(COURSE_META_FILE) ?: return null
        val courseReader = { zipFile.getInputStream(courseEntry).reader(StandardCharsets.UTF_8) }
        val fileContentsFactory: FileContentsFactory = FileContentsFromZipFactory(zipPath.pathString, courseCipher)
        readCourseJson(courseReader, fileContentsFactory)
      }
    }
    catch (_: Exception) {
      null
    }
  }
}
