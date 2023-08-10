package com.jetbrains.edu.learning.projectView

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.pathRelativeToTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.TestOnly
import javax.swing.Icon

object CourseViewUtils {

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
        val path = virtualFile.pathRelativeToTask(project)
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
    val taskDir = getDir(project.courseDir) ?: error("Directory for task $name not found")
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

  fun findTaskDirectory(project: Project, baseDir: PsiDirectory, task: Task): PsiDirectory? {
    val sourceDirName = task.sourceDir
    if (sourceDirName.isNullOrEmpty() || CCUtils.isCourseCreator(project)) {
      return baseDir
    }
    val vFile = baseDir.virtualFile
    val sourceVFile = vFile.findFileByRelativePath(sourceDirName) ?: return baseDir

    if (task.hasVisibleTaskFilesNotInsideSourceDir(project)) {
      return baseDir
    }
    return PsiManager.getInstance(project).findDirectory(sourceVFile)
  }

  @TestOnly
  fun testPresentation(node: AbstractTreeNode<out PsiFileSystemItem>): String {
    val presentation = node.presentation
    val fragments = presentation.coloredText
    val className = node.javaClass.simpleName
    return if (fragments.isEmpty()) {
      "$className ${presentation.presentableText}"
    }
    else {
      fragments.joinToString(separator = "", prefix = "$className ") { it.text }
    }
  }

  fun getIcon(item: StudyItem): Icon {
    return when (item) {
      is Course -> item.icon
      is Section -> {
        if (item.isSolved) EducationalCoreIcons.SectionSolved else EducationalCoreIcons.Section
      }
      is Lesson -> {
        if (item.isSolved) EducationalCoreIcons.LessonSolved else EducationalCoreIcons.Lesson
      }
      is Task -> item.icon
      else -> error("Unexpected item type: ${item.javaClass.simpleName}")
    }
  }

  val StudyItem.isSolved: Boolean
    get() {
      return when (this) {
        is Section -> lessons.all { it.isSolved() }
        is Lesson -> isSolved()
        is Task -> status == CheckStatus.Solved
        else -> false
      }
    }

  private fun Lesson.isSolved() = taskList.all {
    val project = it.project ?: return false
    it.status == CheckStatus.Solved || SubmissionsManager.getInstance(project).containsCorrectSubmission(it.id)
  }

  val Course.icon: Icon
    get() {
      return if (this is CodeforcesCourse) EducationalCoreIcons.CODEFORCES_SMALL
      else EducationalCoreIcons.CourseTree
    }

  val Task.icon: Icon
    get() {
      return when (this) {
        is IdeTask -> if (isSolved) EducationalCoreIcons.IdeTaskSolved else EducationalCoreIcons.IdeTask
        is TheoryTask -> if (isSolved) EducationalCoreIcons.TheoryTaskSolved else EducationalCoreIcons.TheoryTask
        else -> if (status == CheckStatus.Unchecked) EducationalCoreIcons.Task
        else if  (isSolved || containsCorrectSubmissions()) EducationalCoreIcons.TaskSolved
        else EducationalCoreIcons.TaskFailed
      }
    }

  private fun Task.containsCorrectSubmissions(): Boolean {
    val project = course.project ?: return false
    return SubmissionsManager.getInstance(project).containsCorrectSubmission(id)
  }
}
