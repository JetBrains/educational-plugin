package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseDir

@Service(Service.Level.PROJECT)
class CourseIgnoreChecker(val project: Project) {

  val courseIgnoreRules: CourseIgnoreRules
    get() = runReadAction {
      val courseIgnoreVirtualFile = project.courseDir.findChild(EduNames.COURSE_IGNORE)
                                    ?: return@runReadAction CourseIgnoreRules.EMPTY
      val courseIgnorePsiFile = PsiManager.getInstance(project).findFile(courseIgnoreVirtualFile)
                                ?: return@runReadAction CourseIgnoreRules.EMPTY

      CachedValuesManager.getCachedValue(courseIgnorePsiFile, CachedValueProvider {
        CachedValueProvider.Result(
          CourseIgnoreRules.interpret(project, courseIgnorePsiFile),
          PsiModificationTracker.MODIFICATION_COUNT
        )
      })
    }

  fun isIgnored(virtualFile: VirtualFile): Boolean = courseIgnoreRules.isIgnored(virtualFile, project)

  companion object {
    fun instance(project: Project): CourseIgnoreChecker = project.service()
  }
}