package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore.getRelativePath
import com.intellij.psi.*
import com.intellij.ui.LayeredIcon
import com.jetbrains.edu.EducationalCoreIcons.CourseView
import com.jetbrains.edu.EducationalCoreIcons.CourseView.*
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesTaskFileState
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
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
    fileNodeFactory: (AbstractTreeNode<*>, PsiFile) -> AbstractTreeNode<*>,
    directoryNodeFactory: (PsiDirectory) -> AbstractTreeNode<*>,
  ): AbstractTreeNode<*>? {
    if (task == null) {
      val course = project.course ?: return null
      return childNode.modifyAdditionalFileOrDirectoryForLearner(project, course, showUserCreatedFiles = true)
    }

    val value = childNode.value
    return when (value) {
      is PsiDirectory -> {
        val dirName = value.name
        if (dirName == EduNames.BUILD || dirName == EduNames.OUT) return null
        if (isShowDirInView(project, task, value)) directoryNodeFactory(value) else null
      }

      is PsiElement -> {
        val psiFile = value.containingFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        val path = virtualFile.pathRelativeToTask(project)
        val visibleFile = task.getTaskFile(path)
        if (visibleFile?.isVisible == true) fileNodeFactory(childNode, psiFile) else null
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
      if (!it.isVisible) return@any false
      val virtualFile = it.getVirtualFile(project) ?: return@any false
      VfsUtil.isAncestor(dir.virtualFile, virtualFile, true)
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
    val icon: Icon = when (item) {
      is Course -> CourseTree
      is Section -> if (item.isSolved) SectionSolved else Section

      is Lesson -> if (item.isSolved) LessonSolved else Lesson

      is Task -> when (item) {
        is IdeTask -> if (item.isSolved) IdeTaskSolved else CourseView.IdeTask
        is TheoryTask -> if (item.isSolved) TheoryTaskSolved else CourseView.TheoryTask
        else -> if (item.status == CheckStatus.Unchecked) CourseView.Task
        else if (item.isSolved || item.containsCorrectSubmissions()) TaskSolved
        else TaskFailed
      }

      else -> error("Unexpected item type: ${item.javaClass.simpleName}")
    }
    val modifier = getSyncChangesModifier(item) ?: return icon
    return LayeredIcon.create(icon, modifier)
  }

  private fun getSyncChangesModifier(item: StudyItem): Icon? {
    val project = item.course.project ?: return null
    val syncChangesStateManager = SyncChangesStateManager.getInstance(project)
    val state = when (item) {
      is Task -> syncChangesStateManager.getSyncChangesState(item)
      is FrameworkLesson -> syncChangesStateManager.getSyncChangesState(item)
      else -> null
    }
    return when (state) {
      SyncChangesTaskFileState.INFO -> SyncFilesModInfo
      SyncChangesTaskFileState.WARNING -> SyncFilesModWarning
      else -> null
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

  val Task.icon: Icon
    get() {
      return when (this) {
        is IdeTask -> if (isSolved) IdeTaskSolved else CourseView.IdeTask
        is TheoryTask -> if (isSolved) TheoryTaskSolved else CourseView.TheoryTask
        else -> if (status == CheckStatus.Unchecked) CourseView.Task
        else if (isSolved || containsCorrectSubmissions()) TaskSolved
        else TaskFailed
      }
    }

  private fun Task.containsCorrectSubmissions(): Boolean {
    val project = course.project ?: return false
    return SubmissionsManager.getInstance(project).containsCorrectSubmission(id)
  }

  fun ContentHolderNode.createNodeFromPsiDirectory(
    course: Course,
    directory: PsiDirectory,
  ): AbstractTreeNode<*>? {
    val section = course.getSection(directory.name)
    if (section != null) {
      return createSectionNode(directory, section)
    }
    val lesson = course.getLesson(directory.name)
    if (lesson != null) {
      val lessonSolved = lesson.taskList.all { it.status == CheckStatus.Solved }
      if (lessonSolved && PropertiesComponent.getInstance().getBoolean(CourseViewPane.HIDE_SOLVED_LESSONS, false)) {
        return null
      }
      return createLessonNode(directory, lesson)
    }
    if (directory.isPartOfCustomContentPath(getProject())) {
      return createIntermediateDirectoryNode(directory, course)
    }
    return null
  }

  private fun PsiDirectory.isPartOfCustomContentPath(project: Project): Boolean {
    val relativePath = getRelativePath(virtualFile, project.courseDir) ?: return false
    return project.course.customContentPath.contains(relativePath)
  }

  /**
   * Returns some node for visible additional files or directories, and returns `null` otherwise.
   *
   * If [this] corresponds to a file, check that it is a visible additional file and return the node.
   * If [showUserCreatedFiles] is `true`, only invisible additional files are hidden. It means that the tree
   * shows both visible additional files and non-additional files that we created by user after the project has been generated.
   *
   * If [this] corresponds to a directory, check that there exist a visible additional file inside.
   * Return [DirectoryNode] in that case, and return `null` otherwise.
   *
   * For directories, the [com.jetbrains.edu.learning.configuration.EduConfigurator.shouldFileBeVisibleToStudent] check is also performed.
   * In that case the directory node itself is returned to make the entire directory visible.
   * This check will be refactored in EDU-8288.
   */
  fun AbstractTreeNode<*>.modifyAdditionalFileOrDirectoryForLearner(
    project: Project,
    course: Course,
    showUserCreatedFiles: Boolean
  ): AbstractTreeNode<*>? {
    if (!course.isStudy) return null

    return when (this) {
      is PsiFileNode -> modifyAdditionalFileForLearner(project, course, showUserCreatedFiles)
      is PsiDirectoryNode -> modifyAdditionalDirectoryForLearner(project, course)
      else -> null
    }
  }

  private fun PsiFileNode.modifyAdditionalFileForLearner(project: Project, course: Course, showUserCreatedFiles: Boolean): AbstractTreeNode<*>? {
    val nodePsiElement = value ?: return null
    val nodeFile = nodePsiElement.virtualFile ?: return null

    val nodePath = getRelativePath(nodeFile, project.courseDir)

    val additionalFile = course.additionalFiles.find { it.name == nodePath }

    if (additionalFile?.isVisible == true || additionalFile?.isVisible != false && showUserCreatedFiles) {
      return this
    }
    else {
      return null
    }
  }

  private fun PsiDirectoryNode.modifyAdditionalDirectoryForLearner(project: Project, course: Course): AbstractTreeNode<*>? {
    val nodePsiElement = value ?: return null
    val nodeDirectory = nodePsiElement.virtualFile
    //TODO remove in EDU-8288
    if (course.configurator?.shouldFileBeVisibleToStudent(nodeDirectory) == true) return this

    val nodePath = getRelativePath(nodeDirectory, project.courseDir) ?: return null

    return if (course.additionalFiles.find { it.isVisible && it.name.startsWith("$nodePath/") } != null) {
      DirectoryNode(project, nodePsiElement, settings, null)
    }
    else {
      null
    }
  }
}
