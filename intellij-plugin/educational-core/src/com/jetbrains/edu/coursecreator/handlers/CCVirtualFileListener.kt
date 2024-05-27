package com.jetbrains.edu.coursecreator.handlers

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
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
    }
    SyncChangesStateManager.getInstance(project).taskFileCreated(taskFile)
  }

  override fun taskFileChanged(taskFile: TaskFile, file: VirtualFile) {
    SyncChangesStateManager.getInstance(project).taskFileChanged(taskFile)
  }

  override fun fileMoved(event: VFileMoveEvent) {
    super.fileMoved(event)

    val movedFile = event.file
    val fileInfo = movedFile.fileInfo(project) as? FileInfo.FileInTask ?: return
    val oldDirectoryInfo = event.oldParent.directoryFileInfo(project) ?: return

    SyncChangesStateManager.getInstance(project).fileMoved(movedFile, fileInfo, oldDirectoryInfo)
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
    }
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

    val mapper = StudyTaskManager.getInstance(project).course?.mapper ?: YamlMapper.MAPPER
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

    return true
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

  companion object {
    private val LOG: Logger = Logger.getInstance(CCVirtualFileListener::class.java)
    private const val COURSE_REFRESH_REQUEST: String = "Course project refresh request"
  }
}