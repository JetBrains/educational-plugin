package com.jetbrains.edu.learning.projectView

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import icons.EducationalCoreIcons
import javax.swing.Icon

object CourseViewUtils {

  @JvmStatic
  fun modifyTaskChildNode(
    project: Project,
    childNode: AbstractTreeNode<*>,
    task: Task?,
    directoryNodeFactory: (PsiDirectory) -> AbstractTreeNode<*>
  ): AbstractTreeNode<*>? {
    val value = childNode.value
    return when (value) {
      is PsiDirectory -> {
        val dirName = value.name
        if (dirName == EduNames.BUILD || dirName == EduNames.OUT) return null
        if (task != null && isShowDirInView(project, task, value)) directoryNodeFactory(value) else null
      }
      is PsiElement -> {
        val psiFile = value.containingFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        val path = EduUtils.pathRelativeToTask(project, virtualFile)
        val visibleFile = task?.getTaskFile(path)
        if (visibleFile?.isVisible == true) childNode else null
      }
      else -> null
    }
  }

  private fun isShowDirInView(project: Project, task: Task, dir: PsiDirectory): Boolean {
    if (dir.children.isEmpty()) return true
    val dirName = dir.name
    val hasTaskFileNotInsideSourceDir = task.hasVisibleTaskFilesNotInsideSourceDir(project)
    if (dirName == task.sourceDir) return hasTaskFileNotInsideSourceDir
    return task.taskFiles.values.any {
      it.isVisible && VfsUtil.isAncestor(dir.virtualFile, it.getVirtualFile(project)?:return@any false, true)
    }
  }

  private fun Task.hasVisibleTaskFilesNotInsideSourceDir(project: Project): Boolean {
    val taskDir = getDir(project) ?: error("Directory for task $name not found")
    val sourceDir = findSourceDir(taskDir) ?: return false
    return taskFiles.values.any {
      if (!it.isVisible) return@any false
      val virtualFile = it.getVirtualFile(project)
      if (virtualFile == null) {
        Logger.getInstance(Task::class.java).warn("VirtualFile for ${it.name} not found")
        return@any false
      }

      !VfsUtil.isAncestor(sourceDir, virtualFile, true)
    }
  }

  @JvmStatic
  fun findTaskDirectory(project: Project, baseDir: PsiDirectory, task: Task): PsiDirectory? {
    val sourceDirName = task.sourceDir
    if (sourceDirName.isNullOrEmpty() || CCUtils.isCourseCreator(project)) {
      return baseDir
    }
    val vFile = baseDir.virtualFile
    val sourceVFile = vFile.findFileByRelativePath(sourceDirName!!) ?: return baseDir

    if (task.hasVisibleTaskFilesNotInsideSourceDir(project)) {
      return baseDir
    }
    return PsiManager.getInstance(project).findDirectory(sourceVFile)
  }

  @JvmStatic
  fun testPresentation(node: AbstractTreeNode<out PsiFileSystemItem>): String {
    val presentation = node.presentation
    val fragments = presentation.coloredText
    val className = node.javaClass.simpleName
    return if (fragments.isEmpty()) {
      "$className ${node.value.name}"
    }
    else {
      fragments.joinToString(separator = "", prefix = "$className ") { it.text }
    }
  }

  @JvmStatic
  val StudyItem.additionalInformation: String?
    get() {
      if (this is Course) {
        return if (course.isStudy) {
          val progress = ProgressUtil.countProgress(this)
          val tasksSolved = progress.first
          val tasksTotal = progress.second
          " $tasksSolved/$tasksTotal"
        }
        else {
          "(Course Creation)"
        }
      }

      return if (!course.isStudy && presentableName != name) "($name)" else null
    }

  @JvmStatic
  val StudyItem.icon: Icon
    get() {
      return when (this) {
        is Course -> {
          EducationalCoreIcons.CourseTree
        }
        is Section -> {
          if (isSolved) EducationalCoreIcons.SectionSolved else EducationalCoreIcons.Section
        }
        is Lesson -> {
          if (isSolved) EducationalCoreIcons.LessonSolved else EducationalCoreIcons.Lesson
        }
        is Task -> {
          if (isSolved) EducationalCoreIcons.TaskSolved else EducationalCoreIcons.Task
        }
        else -> error("Unexpected item type: ${this.javaClass.simpleName}")
      }
    }

  private val StudyItem.isSolved: Boolean
    get() {
      return when (this) {
        is Section -> lessons.all { it.isSolved() }
        is Lesson -> isSolved()
        is Task -> status == CheckStatus.Solved
        else -> false
      }
    }

  private fun Lesson.isSolved() = taskList.all { it.status == CheckStatus.Solved }
}
