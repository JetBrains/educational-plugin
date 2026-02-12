package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.ui.LayeredIcon
import com.jetbrains.edu.EducationalCoreIcons.CourseView
import com.jetbrains.edu.EducationalCoreIcons.CourseView.*
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesTaskFileState
import com.jetbrains.edu.coursecreator.projectView.CCNode
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
import com.jetbrains.edu.learning.configuration.courseFileAttributes
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
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.Path
import javax.swing.Icon

object CourseViewUtils {

  fun modifyTaskChildNode(
    project: Project,
    childNode: AbstractTreeNode<*>,
    task: Task?,
    fileNodeFactory: (AbstractTreeNode<*>, PsiFile) -> AbstractTreeNode<*>,
    directoryNodeFactory: (PsiDirectory) -> AbstractTreeNode<*>,
  ): AbstractTreeNode<*>? {
    val course = project.course ?: return null
    val visibility = childNode.courseViewVisibilityAttribute(project, course)
    if (visibility == CourseViewVisibility.INVISIBLE_FOR_ALL) return null

    if (task == null) {
      val showUserCreatedFiles = isCourseWithVisibleUserFiles(course)
      return childNode.modifyAdditionalFileOrDirectory(project, course, showUserCreatedFiles)
    }

    return when (val value = childNode.value) {
      is PsiDirectory -> {
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
    val relativePath = virtualFile.pathInCourse(project) ?: return false
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
   */
  fun AbstractTreeNode<*>.modifyAdditionalFileOrDirectory(
    project: Project,
    course: Course,
    showUserCreatedFiles: Boolean
  ): AbstractTreeNode<*>? = if (course.isStudy) {
    modifyAdditionalFileOrDirectoryForLearner(project, course, showUserCreatedFiles)
  }
  else {
    modifyAdditionalFileOrDirectoryForTeacher(project, course)
  }

  private fun AbstractTreeNode<*>.modifyAdditionalFileOrDirectoryForLearner(
    project: Project,
    course: Course,
    showUserCreatedFiles: Boolean
  ): AbstractTreeNode<*>? {
    return when (this) {
      is PsiFileNode -> modifyAdditionalFile(project, course, showUserCreatedFiles)
      is PsiDirectoryNode -> modifyAdditionalDirectory(project, course, showUserCreatedFiles) {
        DirectoryNode(project, it, settings, null)
      }
      else -> null
    }
  }

  private fun AbstractTreeNode<*>.modifyAdditionalFileOrDirectoryForTeacher(project: Project, course: Course): AbstractTreeNode<*>? {
    return when (this) {
      is PsiFileNode -> modifyAdditionalFile(project, course, showUserCreatedFiles = false)
      is PsiDirectoryNode -> modifyAdditionalDirectory(project, course, showUserCreatedFiles = false) {
        CCNode(project, it, settings, null)
      }
      else -> null
    }
  }


  private fun PsiFileNode.modifyAdditionalFile(project: Project, course: Course, showUserCreatedFiles: Boolean): AbstractTreeNode<*>? {
    val nodePsiElement = value ?: return null
    val nodeFile = nodePsiElement.virtualFile ?: return null

    val nodePath = nodeFile.pathInCourse(project) ?: return null

    val additionalFile = course.getAdditionalFile(nodePath)

    if (additionalFile?.isVisible == true || additionalFile?.isVisible != false && showUserCreatedFiles) {
      return this
    }
    else {
      return null
    }
  }

  private fun PsiDirectoryNode.modifyAdditionalDirectory(
    project: Project,
    course: Course,
    showUserCreatedFiles: Boolean,
    directoryNodeBuilder: (PsiDirectory) -> DirectoryNode
  ): AbstractTreeNode<*>? {
    val nodePsiElement = value ?: return null
    val nodeDirectory = nodePsiElement.virtualFile

    if (courseViewVisibilityAttribute(project, course) == CourseViewVisibility.VISIBLE_FOR_STUDENT) return this

    val nodePath = nodeDirectory.pathInCourse(project) ?: return null

    return if (shouldShowDirectory(course, nodePath, showUserCreatedFiles)) {
      directoryNodeBuilder(nodePsiElement)
    }
    else {
      null
    }
  }

  private fun shouldShowDirectory(course: Course, nodePath: String, showUserCreatedFiles: Boolean): Boolean {
    val containsVisibleAdditionalFile = course.additionalFiles.any { it.isVisible && it.name.startsWith("$nodePath/") }

    if (containsVisibleAdditionalFile) return true
    if (!showUserCreatedFiles) return false

    // In case we show user-created files and directories (see CourseViewUtils.modifyAdditionalFileOrDirectory), determine whether this
    // directory is inside some visible additional-directory, i.e., a directory with a visible additional file.

    val isUnderVisibleAdditionalFile = course.additionalFiles.any {
      it.isVisible && nodePath.isInsideDirectoryOf(it)
    }

    return isUnderVisibleAdditionalFile
  }

  private fun String.isInsideDirectoryOf(file: EduFile): Boolean {
    val parentDir = Path.of(file.name).parent ?: return true
    return Path.of(this).startsWith(parentDir)
  }

  fun AbstractTreeNode<*>.courseViewVisibilityAttribute(project: Project, course: Course): CourseViewVisibility {
    if (this !is PsiDirectoryNode && this !is PsiFileNode) return CourseViewVisibility.INVISIBLE_FOR_ALL
    val virtualFile = this.virtualFile ?: return CourseViewVisibility.INVISIBLE_FOR_ALL
    val configurator = course.configurator ?: return CourseViewVisibility.INVISIBLE_FOR_ALL
    return configurator.courseFileAttributes(project, virtualFile).visibility
  }

  @VisibleForTesting
  val COURSES_WITH_VISIBLE_USER_FILES = setOf(
    28816, // Mastering Large Language Models
    205112, // (AWS) Mastering Large Language Models
  )

  /**
   * Normally, we should not show files created by users outside of task directories. But for some courses we do it because we have no
   * idea on how to implement them some other way. See EDU-8717. These courses are hard-coded inside this method.
   */
  private fun isCourseWithVisibleUserFiles(course: Course): Boolean {
    // TODO this Junie course is still not published, so we don't know its id by now
    val hasJunieSubdirectory = course.additionalFiles.any { it.isVisible && it.name == ".junie/guidelines.md" }

    return hasJunieSubdirectory || course.id in COURSES_WITH_VISIBLE_USER_FILES
  }
}
