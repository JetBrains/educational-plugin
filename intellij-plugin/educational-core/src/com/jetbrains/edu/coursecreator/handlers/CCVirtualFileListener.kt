package com.jetbrains.edu.coursecreator.handlers

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils.isExcluded
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.handlers.EduVirtualFileListener
import com.jetbrains.edu.learning.yaml.*
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlLoader.deserializeChildrenIfNeeded

class CCVirtualFileListener(project: Project, parentDisposable: Disposable) : EduVirtualFileListener(project) {

  private val projectRefreshRequestsQueue = MergingUpdateQueue(COURSE_REFRESH_REQUEST,
                                                               300,
                                                               true,
                                                               null,
                                                               parentDisposable).apply { setRestartTimerOnAdd(true) }

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    if (file.isTestsFile(project) || file.isTaskRunConfigurationFile(project)) {
      taskFile.isVisible = false
      taskFile.isPropagatable = false
    }
    SyncChangesStateManager.getInstance(project).taskFileCreated(taskFile)
  }

  override fun taskFileChanged(taskFile: TaskFile, file: VirtualFile) {
    SyncChangesStateManager.getInstance(project).taskFileChanged(taskFile)
  }

  override fun fileMoved(event: VFileMoveEvent) {
    super.fileMoved(event)

    val movedFile = event.file

    val oldParentInfo = event.oldParent.outsideOfTaskInfo(project)
    val newParentInfo = event.newParent.outsideOfTaskInfo(project)

    when {
      oldParentInfo is FileInfo.FileOutsideTasks && newParentInfo is FileInfo.FileOutsideTasks -> {
        moveAdditionalFiles(oldParentInfo.appendPath(movedFile.name), movedFile)
      }
      oldParentInfo is FileInfo.FileOutsideTasks -> {
        val fileInfo = oldParentInfo.appendPath(movedFile.name)
        deleteAdditionalFile(fileInfo)
      }
      newParentInfo is FileInfo.FileOutsideTasks -> {
        addFileIfAdditionalRecursively(movedFile)
      }
    }

    val fileInfo = movedFile.fileInfo(project) as? FileInfo.FileInTask ?: return
    val oldDirectoryInfo = event.oldParent.directoryFileInfo(project) ?: return

    SyncChangesStateManager.getInstance(project).fileMoved(movedFile, fileInfo, oldDirectoryInfo)
  }

  private fun moveAdditionalFiles(oldFileInfo: FileInfo.FileOutsideTasks, movedFile: VirtualFile) {
    val course = oldFileInfo.course

    if (movedFile.isDirectory) {
      val additionalFilesPaths = course.additionalFiles.map { it.name }.toSet()

      VfsUtil.visitChildrenRecursively(movedFile, object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
        override fun visitFile(file: VirtualFile): Boolean {
          if (!file.isDirectory) {
            val relativePath = VfsUtil.findRelativePath(movedFile, file, VFS_SEPARATOR_CHAR) ?: return true
            val visitedFileOldInfo = oldFileInfo.appendPath(relativePath)
            if (visitedFileOldInfo.coursePath in additionalFilesPaths) {
              addFileIfAdditional(file, evenIfExcluded = true)
              deleteAdditionalFile(visitedFileOldInfo)
            }
          }
          return true
        }
      })
    }
    else {
      addFileIfAdditional(movedFile, evenIfExcluded = true)
      deleteAdditionalFile(FileInfo.FileOutsideTasks(course, oldFileInfo.coursePath))
    }
  }

  override fun fileCreated(file: VirtualFile) {
    super.fileCreated(file)
    addFileIfAdditional(file)
  }

  private fun addFileIfAdditionalRecursively(file: VirtualFile) {
    VfsUtil.visitChildrenRecursively(file, object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      override fun visitFile(file: VirtualFile): Boolean {
        if (!file.isDirectory) {
          addFileIfAdditional(file)
        }
        return true
      }
    })
  }

  private fun addFileIfAdditional(file: VirtualFile, evenIfExcluded: Boolean = false) {
    if (file.isDirectory) return

    val containingTask = file.getContainingTask(project)
    if (containingTask != null) return

    val course = project.course ?: return
    val configurator = course.configurator ?: return
    val isExcluded = !evenIfExcluded && isExcluded(
      file,
      CourseIgnoreRules.loadFromCourseIgnoreFile(project),
      configurator,
      project
    )
    if (!isExcluded) {
      additionalFileCreated(course, file)
    }
  }

  private fun additionalFileCreated(course: Course, file: VirtualFile) {
    val name = VfsUtil.getRelativePath(file, project.courseDir) ?: return
    if (course.additionalFiles.any { it.name == name }) return
    course.additionalFiles += EduFile(name, if (file.isToEncodeContent) {
      BinaryContents.EMPTY
    }
    else {
      TextualContents.EMPTY
    })
    YamlFormatSynchronizer.saveItem(course)
  }

  override fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {
    when (fileInfo) {
      is FileInfo.SectionDirectory -> {
        deleteSection(fileInfo)
        refreshCourse(fileInfo.section.course)
      }

      is FileInfo.LessonDirectory -> {
        deleteLesson(fileInfo)
        refreshCourse(fileInfo.lesson.course)
      }

      is FileInfo.TaskDirectory -> {
        deleteTask(fileInfo)
        refreshCourse(fileInfo.task.course)
      }

      is FileInfo.FileInTask -> deleteFileInTask(fileInfo, file)

      is FileInfo.FileOutsideTasks -> deleteAdditionalFile(fileInfo)
    }
  }

  private fun deleteAdditionalFile(fileInfo: FileInfo.FileOutsideTasks) {
    val course = fileInfo.course
    val coursePath = fileInfo.coursePath
    course.additionalFiles = course.additionalFiles.filter { additionalFile ->
      val name = additionalFile.name
      name != coursePath && !coursePath.isParentOf(name)
    }
    YamlFormatSynchronizer.saveItem(course)
  }

  override fun beforePropertyChange(event: VFilePropertyChangeEvent) {
    super.beforePropertyChange(event)

    if (event.propertyName != VirtualFile.PROP_NAME) return
    val info = event.file.outsideOfTaskInfo(project) ?: return
    val newName = event.newValue as? String ?: return

    val course = info.course
    val oldPath = info.coursePath
    val newPath = oldPath.replaceAfterLast(VFS_SEPARATOR_CHAR, newName, newName)

    for (additionalFile in course.additionalFiles) {
      val name = additionalFile.name

      additionalFile.name = when {
        name == oldPath -> newPath // if the file itself is renamed
        oldPath.isParentOf(name) -> newPath + name.substringAfter(oldPath, "") // if the parent folder is renamed
        else -> name
      }
    }

    YamlFormatSynchronizer.saveItem(course)
  }

  private fun deleteLesson(info: FileInfo.LessonDirectory) {
    val removedLesson = info.lesson
    val course = removedLesson.course
    val section = removedLesson.section
    CCFrameworkLessonManager.getInstance(project).removeRecords(removedLesson)
    if (section != null) {
      section.removeLesson(removedLesson)
      YamlFormatSynchronizer.saveItem(section)
    }
    else {
      course.removeLesson(removedLesson)
      YamlFormatSynchronizer.saveItem(course)
    }
  }

  private fun deleteSection(info: FileInfo.SectionDirectory) {
    val removedSection = info.section
    val course = removedSection.course
    course.removeSection(removedSection)
    CCFrameworkLessonManager.getInstance(project).removeRecords(removedSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun deleteTask(info: FileInfo.TaskDirectory) {
    val task = info.task
    val lesson = task.lesson
    lesson.removeTask(task)
    SyncChangesStateManager.getInstance(project).taskDeleted(task)
    CCFrameworkLessonManager.getInstance(project).removeRecord(task)
    YamlFormatSynchronizer.saveItem(lesson)
  }

  private fun deleteFileInTask(info: FileInfo.FileInTask, file: VirtualFile) {
    val (task, pathInTask) = info

    if (file.isDirectory) {
      val toRemove = task.taskFiles.keys.filter { pathInTask.isParentOf(it) }
      for (path in toRemove) {
        task.removeTaskFile(path)
      }
      SyncChangesStateManager.getInstance(project).filesDeleted(task, toRemove)
    }
    else {
      task.removeTaskFile(pathInTask)
      SyncChangesStateManager.getInstance(project).filesDeleted(task, listOf(pathInTask))
    }
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun beforeFileDeletion(event: VFileDeleteEvent) {
    val fileInfo = event.file.fileInfo(project) ?: return

    val studyItem = when (fileInfo) {
      is FileInfo.SectionDirectory -> fileInfo.section
      is FileInfo.LessonDirectory -> fileInfo.lesson
      is FileInfo.TaskDirectory -> fileInfo.task
      else -> return
    }

    val courseBuilder = studyItem.course.configurator?.courseBuilder ?: return
    courseBuilder.beforeStudyItemDeletion(project, studyItem)
  }

  override fun configUpdated(configEvents: List<VFileEvent>) {
    val sortedConfigEvents = configEvents.sortedWith(compareConfigs)
    var needRefreshProject = false

    for (event in sortedConfigEvents) {
      if (processOneConfigEvent(event)) {
        needRefreshProject = true
      }
    }

    if (needRefreshProject) {
      refreshCourse(project.course)
    }
  }

  /**
   * returns whether the course needs to be refreshed
   */
  private fun processOneConfigEvent(event: VFileEvent): Boolean {
    val createdConfigFile = when (event) {
      is VFileCreateEvent, is VFileMoveEvent -> event.file
      is VFileCopyEvent -> event.newParent.findChild(event.newChildName)
      else -> null
    }

    if (createdConfigFile != null) {
      return configCreated(createdConfigFile)
    }

    if (event is VFileContentChangeEvent) {
      return configChanged(event)
    }

    return false
  }

  private fun configChanged(event: VFileContentChangeEvent): Boolean {
    val configAdded = tryAddItemToParentConfig(event.file)
    reloadConfig(event.file)
    return configAdded
  }

  private fun configCreated(configFile: VirtualFile): Boolean {
    val configAdded = tryAddItemToParentConfig(configFile)
    if (configAdded) {
      reloadConfig(configFile)
    }

    return configAdded
  }

  /**
   * returns whether the config was successfully added
   */
  private fun tryAddItemToParentConfig(configFile: VirtualFile): Boolean {
    val itemDir = configFile.parent ?: return false
    val parentItemDir = itemDir.parent ?: return false
    val parentStudyItem = parentItemDir.getStudyItem(project)

    if (parentStudyItem == null) {
      if (configFile.name != YamlConfigSettings.COURSE_CONFIG)
        LOG.warn("Study item configuration file was created without a parent study item: $configFile. This could be a temporary issue, if a parent item is created soon.")
      return false
    }

    //if the study item is already inside the parent, do not add it
    if (parentStudyItem is ItemContainer) {
      val previousItem = parentStudyItem.getItem(itemDir.name)
      if (previousItem != null) {
        if (previousItem.configFileName != configFile.name) {
          LOG.warn("Study item configuration file was created near another study item configuration file: $configFile")
        }

        return false
      }
    }

    val mapper = StudyTaskManager.getInstance(project).course?.mapper() ?: YamlMapper.basicMapper()
    val deserializedItem = deserializeItemProcessingErrors(configFile, project, true, mapper) ?: return false

    if (!deserializedItem.couldBeInside(parentStudyItem)) {
      LOG.warn("Study item configuration file was created in a child folder of another study item, but the upper study item can not contain the created one: $configFile")
      return false
    }

    if (parentStudyItem !is ItemContainer) return false // this is mostly to cast the type, because the actual check is already performed

    deserializedItem.name = itemDir.name
    deserializedItem.parent = parentStudyItem
    deserializedItem.deserializeChildrenIfNeeded(project, parentStudyItem.course)
    deserializedItem.init(parentStudyItem, false)
    parentStudyItem.addItem(deserializedItem)
    deserializedItem.index = 1 + parentStudyItem.items.indexOf(deserializedItem)

    YamlFormatSynchronizer.saveItem(parentStudyItem)

    // If a Task is created and added to the course, make sure that files from the task folder are not listed as additional files.
    // It may happen on task copy, because files are created on disk and automatically added to the list of additional files before
    // the Task object is created.
    if (deserializedItem is Task) {
      removeTaskFilesFromAdditionalFiles(deserializedItem)
    }
    return true
  }

  private fun removeTaskFilesFromAdditionalFiles(task: Task) {
    val course = task.course
    val pathPrefix = task.pathInCourse + VFS_SEPARATOR_CHAR

    val withoutFilesFromTask = course.additionalFiles.filter {
      !it.name.startsWith(pathPrefix)
    }

    if (withoutFilesFromTask.size < course.additionalFiles.size) {
      course.additionalFiles = withoutFilesFromTask
      YamlFormatSynchronizer.saveItem(course)
    }
  }

  private fun reloadConfig(file: VirtualFile) {
    if (file.length == 0L) return
    val loadFromConfig = file.getUserData(YamlFormatSynchronizer.LOAD_FROM_CONFIG) ?: true
    if (loadFromConfig) {
      runInEdt {
        YamlLoader.loadItem(project, file, true)
        ProjectView.getInstance(project).refresh()
      }
    }
  }

  private fun StudyItem.couldBeInside(parent: StudyItem): Boolean = when (this) {
    is Course -> false
    is Section -> parent is Course
    is Lesson -> parent is Course || parent is Section
    is Task -> parent is Lesson
    else -> false
  }

  private val compareConfigs: Comparator<VFileEvent> = Comparator.comparingInt {
    when (it?.file?.name) {
      YamlConfigSettings.COURSE_CONFIG -> 0
      YamlConfigSettings.SECTION_CONFIG -> 1
      YamlConfigSettings.LESSON_CONFIG -> 2
      YamlConfigSettings.TASK_CONFIG -> 3
      else -> 4
    }
  }

  private fun refreshCourse(course: Course?) {
    LOG.info("Requesting project refresh after yaml files update")
    projectRefreshRequestsQueue.queue(Update.create(COURSE_REFRESH_REQUEST) {
      LOG.info("Do actual project refresh after yaml files update")
      course?.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    })
  }

  private fun VirtualFile.outsideOfTaskInfo(project: Project): FileInfo.FileOutsideTasks? {
    val info = fileInfo(project) ?: return null
    return when (info) {
      is FileInfo.FileOutsideTasks -> info
      is FileInfo.SectionDirectory -> FileInfo.FileOutsideTasks(info.section.course, info.section.pathInCourse)
      is FileInfo.LessonDirectory -> FileInfo.FileOutsideTasks(info.lesson.course, info.lesson.pathInCourse)
      else -> null
    }
  }

  fun FileInfo.FileOutsideTasks.appendPath(path: String): FileInfo.FileOutsideTasks {
    val newPath = FileUtil.toCanonicalPath("$coursePath$VFS_SEPARATOR_CHAR$path", VFS_SEPARATOR_CHAR)
    return FileInfo.FileOutsideTasks(course, newPath)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCVirtualFileListener::class.java)
    private const val COURSE_REFRESH_REQUEST: String = "Course project refresh request"
  }
}