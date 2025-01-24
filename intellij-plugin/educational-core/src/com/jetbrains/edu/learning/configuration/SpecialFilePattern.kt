package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course

fun buildArchiveFileInfo(
  holder: CourseInfoHolder<out Course?>,
  file: VirtualFile,
  actions: ArchiveInfoBuilder.() -> Unit
): ArchiveFileInfo =
  ArchiveInfoBuilder(holder, file).apply(actions).generate()

class ArchiveInfoBuilder(
  private val holder: CourseInfoHolder<out Course?>,
  private val file: VirtualFile
) {

  private val info = Info(
    excludedFromArchive=false
  )

  fun excludeFromArchive() {
    info.excludedFromArchive = true
  }

  fun use(newInfo: ArchiveFileInfo) = with(newInfo) {
    info.excludedFromArchive = excludedFromArchive
  }

  fun generate(): ArchiveFileInfo = info

  /**
   * Returns `true`, if the file path relative to the course directory contains a match to the specified regular expression.
   * Use `^` and `$` to specify the beginning and the ending of the path correspondingly.
   * Paths of folders have the `/` character at the end.
   */
  fun regex(pattern: String): Boolean = pathFilter {
    Regex(pattern).containsMatchIn(it)
  }

  /**
   * One of the folders in the path has the specified name
   */
  fun hasFolder(name: String): Boolean = regex("(^|/)${Regex.escape(name)}/")

  /**
   * One of the folders in the path fully matches the specified regular expression.
   */
  fun hasFolderRegex(regex: String): Boolean = regex("(^|/)$regex/")

  private fun pathFilter(filter: (String) -> Boolean): Boolean {
    val path = FileUtil.getRelativePath(holder.courseDir.path, file.path, VFS_SEPARATOR_CHAR) ?: return false

    val fixedPath = if (path == ".") {
      "/"
    }
    else {
      path + if (file.isDirectory) "/" else ""
    }

    return filter(fixedPath)
  }
}

private class Info(
  override var excludedFromArchive: Boolean
) : ArchiveFileInfo