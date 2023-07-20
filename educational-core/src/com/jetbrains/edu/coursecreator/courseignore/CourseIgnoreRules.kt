package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ignore.cache.PatternCache
import com.intellij.openapi.vcs.changes.ignore.psi.IgnoreEntry
import com.intellij.openapi.vcs.changes.ignore.psi.IgnoreVisitor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.EduNames.COURSE_IGNORE
import com.jetbrains.edu.learning.courseDir
import java.util.regex.Pattern

private data class IgnorePattern(val pattern: Pattern, val isNegated: Boolean)

class CourseIgnoreRules private constructor(private val reversedListOfPatterns: List<IgnorePattern>) {

  private fun isIgnored(path: String): Boolean {
    for ((pattern, isNegated) in reversedListOfPatterns) {
      if (pattern.matcher(path).find()) {
        return !isNegated
      }
    }

    return false
  }

  fun isIgnored(file: VirtualFile, project: Project): Boolean {
    val courseRelativePath = VfsUtil.getRelativePath(file, project.courseDir) ?: return false

    val courseRelativePathFixedForDirectory = if (file.isDirectory) {
      "$courseRelativePath/"
    }
    else {
      courseRelativePath
    }

    return isIgnored(courseRelativePathFixedForDirectory)
  }

  companion object {

    private val EMPTY: CourseIgnoreRules = CourseIgnoreRules(listOf())

    fun createFromCourseignoreFile(project: Project): CourseIgnoreRules = runReadAction {
      val courseIgnoreVirtualFile = project.courseDir.findChild(COURSE_IGNORE) ?: return@runReadAction EMPTY
      val courseIgnorePsiFile = PsiManager.getInstance(project).findFile(courseIgnoreVirtualFile) ?: return@runReadAction EMPTY
      interpret(project, courseIgnorePsiFile)
    }

    private fun interpret(project: Project, courseIgnorePsiFile: PsiFile): CourseIgnoreRules {
      val patternCache = PatternCache.getInstance(project)
      val patterns = mutableListOf<IgnorePattern>()

      courseIgnorePsiFile.acceptChildren(object : IgnoreVisitor() {
        override fun visitEntry(ignoreEntry: IgnoreEntry) {
          super.visitEntry(ignoreEntry)
          val pattern = patternCache.createPattern(ignoreEntry) ?: return
          patterns.add(IgnorePattern(pattern, ignoreEntry.isNegated))
        }
      })

      return CourseIgnoreRules(patterns.reversed())
    }
  }
}