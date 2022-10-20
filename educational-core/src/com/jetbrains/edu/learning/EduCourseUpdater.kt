package com.jetbrains.edu.learning

import com.google.common.collect.Lists
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.findDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException
import java.net.URISyntaxException

abstract class EduCourseUpdater(val project: Project, val course: EduCourse) {

  private val oldLessonDirectories = HashMap<Int, VirtualFile>()
  private val oldSectionDirectories = HashMap<Int, VirtualFile>()

  abstract fun courseFromServer(currentCourse: EduCourse, courseInfo: EduCourse?): EduCourse?

  abstract fun taskChanged(newTask: Task, task: Task): Boolean

  open fun processLessonsAfterUpdate(lessonsFromServer: List<Lesson>, courseLessonsById: Map<Int, Lesson>) {}

  abstract fun sectionShouldBeSkipped(sectionId: Int): Boolean

  fun updateCourse(courseInfo: EduCourse? = null) {
    checkIsBackgroundThread()
    oldLessonDirectories.clear()
    oldSectionDirectories.clear()

    val courseFromServer = courseFromServer(course, courseInfo)

    if (courseFromServer == null) {
      val platformName = if (course.isMarketplace) MARKETPLACE else STEPIK
      LOG.warn("Course ${course.id} not found on $platformName")
      return
    }

    doUpdate(courseFromServer)

    runInEdt {
      course.isUpToDate = true
      EduUtils.synchronize()
      ProjectView.getInstance(project).refresh()
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
      YamlFormatSynchronizer.saveAll(project)
    }
  }

  open fun doUpdate(courseFromServer: EduCourse) {
    courseFromServer.items.withIndex().forEach { (index, item) -> item.index = index + 1 }

    setCourseInfo(courseFromServer)
    updateSections(courseFromServer)
    updateLessons(courseFromServer)
    updateAdditionalMaterialsFiles(courseFromServer)
    setCourseItems(courseFromServer.items)
    setUpdated(courseFromServer)

    //remove editor notification, suggesting to update course
    EditorNotifications.getInstance(project).updateAllNotifications()
  }

  protected open fun setUpdated(courseFromServer: EduCourse) {
    course.updateDate = courseFromServer.updateDate
  }

  private fun setCourseItems(remoteItems: List<StudyItem>) {
    course.items = Lists.newArrayList(remoteItems)
  }

  private fun setCourseInfo(courseFromServer: Course) {
    course.name = courseFromServer.name
    course.description = courseFromServer.description
  }

  open fun updateSections(courseFromServer: EduCourse) {
    val sectionIds = course.sections.map { it.id }

    processNewSections(courseFromServer, sectionIds)
    processDeletedSections(courseFromServer)
    processModifiedSections(courseFromServer, sectionIds)
  }

  private fun processNewSections(courseFromServer: Course, sectionIds: List<Int>) {
    val newSections = courseFromServer.sections.filter { section -> section.id !in sectionIds }

    if (newSections.isNotEmpty()) {
      createNewSections(project, newSections)
    }
  }

  private fun processDeletedSections(courseFromServer: EduCourse) {
    deleteRemovedItems(courseFromServer.sections.map { it.id }, course.sections)
  }

  @Throws(URISyntaxException::class, IOException::class)
  private fun processModifiedSections(courseFromServer: EduCourse, sectionIds: List<Int>) {
    val modifiedSectionsFromServerCandidate = courseFromServer.sections.filter { section -> section.id in sectionIds }
    val sectionsById = course.sections.associateBy { it.id }
    for (sectionFromServer in modifiedSectionsFromServerCandidate) {
      sectionFromServer.lessons.withIndex().forEach { (index, lesson) -> lesson.index = index + 1 }

      val sectionFromServerId = sectionFromServer.id
      if (sectionShouldBeSkipped(sectionFromServerId)) return

      val currentSection = sectionsById[sectionFromServerId] ?: error("Section with id $sectionFromServerId not found in local course")
      val currentLessonsIds = currentSection.lessons.map { it.id }

      val newLessons = sectionFromServer.lessons.filter { it.id !in currentLessonsIds }

      val sectionContentChanged = newLessons.isNotEmpty()
      if (sectionContentChanged) {
        constructDir(sectionFromServer, currentSection)
        createNewLessons(newLessons, currentSection)
      }
      else if (renamed(currentSection, sectionFromServer)) {
        constructDir(sectionFromServer, currentSection)
      }

      val lessonsToUpdate = sectionFromServer.lessons.filter { it.id in currentLessonsIds }
      // lessons custom names should be copied before processModifiedLessons, otherwise lesson can be considered modified
      // because of difference in names
      processLessonsAfterUpdate(sectionFromServer.lessons, currentSection.lessons.associateBy { it.id })
      processModifiedLessons(lessonsToUpdate, currentSection)
      sectionFromServer.init(course, false)
    }
  }

  private fun createNewSections(project: Project, newSections: List<Section>) {
    val baseDir = project.courseDir
    for (section in newSections) {
      val sectionDir = baseDir.findChild(section.name)
      if (sectionDir != null) {
        saveExistingDirectory(sectionDir, section)
      }

      section.init(course, false)
      GeneratorUtils.createSection(project, section, baseDir)
    }
  }

  open fun updateLessons(courseFromServer: EduCourse) {
    processNewLessons(courseFromServer)
    processDeletedLessons(courseFromServer)
    processModifiedLessons(courseFromServer.lessons.filter { course.getLesson(it.id) != null }, course)
  }

  private fun processNewLessons(courseFromServer: Course) {
    val newLessons = courseFromServer.lessons.filter { course.getLesson(it.id) == null }
    if (newLessons.isNotEmpty()) {
      createNewLessons(newLessons, course)
    }
  }

  // we are deleting all course lessons in processDeletedLessons(), both inside and outside sections,
  // to correctly process the case when lesson was moved from one section to another
  private fun processDeletedLessons(courseFromServer: EduCourse) {
    val lessonsFromServerIds = courseFromServer.allLessons().map { lesson -> lesson.id }

    val sectionsFromServerIds = courseFromServer.sections.map { section -> section.id }
    val courseLessonsToProcess = course.allLessons().filter {
      sectionsFromServerIds.isEmpty() || it.section?.id in sectionsFromServerIds
    }

    deleteRemovedItems(lessonsFromServerIds, courseLessonsToProcess)
  }

  private fun EduCourse.allLessons(): List<Lesson> = lessons + sections.flatMap { it.lessons }

  @Throws(URISyntaxException::class, IOException::class)
  private fun processModifiedLessons(lessonsFromServer: List<Lesson>, parent: LessonContainer) {
    for (lessonFromServer in lessonsFromServer) {
      lessonFromServer.parent = parent
      lessonFromServer.taskList.withIndex().forEach { (index, task) -> task.index = index + 1 }
      val lessonFromServerId = lessonFromServer.id
      val currentLesson = parent.getLesson(lessonFromServerId) ?: error("Local lesson with id $lessonFromServerId not found")

      val taskIdsToUpdate = taskIdsToUpdate(lessonFromServer, currentLesson)

      deleteRemovedItems(lessonFromServer.taskList.map { task -> task.id }, currentLesson.taskList)

      val lessonContentChanged = taskIdsToUpdate.isNotEmpty()
      if (lessonContentChanged) {
        val lessonDir = constructDir(lessonFromServer, currentLesson)
        updateTasks(taskIdsToUpdate, lessonFromServer, currentLesson, lessonDir)
      }
      else if (renamed(lessonFromServer, currentLesson)) {
        constructDir(lessonFromServer, currentLesson)
      }

      lessonFromServer.init(parent, false)
    }
  }

  @Throws(URISyntaxException::class, IOException::class)
  private fun taskIdsToUpdate(lessonFromServer: Lesson, currentLesson: Lesson): List<Int> {
    val taskIds = lessonFromServer.taskList.map { task -> task.id }
    val tasksById = currentLesson.taskList.associateBy({ it.id }, { it })

    return lessonFromServer.taskList
      .zip(taskIds)
      .filter { (newTask, taskId) ->
        val task = tasksById[taskId]
        task == null || taskChanged(newTask, task)
      }
      .map { (_, taskId) -> taskId }
  }

  private fun createNewLessons(newLessons: List<Lesson>, parentItem: ItemContainer) {
    val parentDir = parentItem.getDir(project.courseDir) ?: error("Failed to get dir for parent item")
    for (lesson in newLessons) {
      lesson.init(parentItem, false)
      val lessonDir = lesson.getDir(project.courseDir)
      if (lessonDir != null) {
        saveExistingDirectory(lessonDir, lesson)
      }

      GeneratorUtils.createLesson(project, lesson, parentDir)
    }
  }

  private fun updateTasks(taskIdsToUpdate: List<Int>,
                          lessonFromServer: Lesson,
                          currentLesson: Lesson,
                          lessonDir: VirtualFile) {
    val serverTasksById = lessonFromServer.taskList.associateBy({ it.id }, { it })
    val tasksById = currentLesson.taskList.associateBy { it.id }
    for (taskId in taskIdsToUpdate) {
      val taskFromServer = serverTasksById[taskId] ?: error("task from server should not be null")
      val currentTask = tasksById[taskId]
      if (currentTask != null) {
        removeExistingDir(currentTask, lessonDir)
      }

      taskFromServer.init(currentLesson, false)

      createTaskDirectories(project, lessonDir, taskFromServer)
    }
  }

  private fun updateAdditionalMaterialsFiles(courseFromServer: Course) {
    val filesToCreate = courseFromServer.additionalFiles
    val baseDir = project.courseDir
    for (file in filesToCreate) {
      GeneratorUtils.createChildFile(project, baseDir, file.name, file.text)
    }
  }

  @Throws(IOException::class)
  private fun removeExistingDir(studentTask: Task, lessonDir: VirtualFile?) {
    val taskDir = studentTask.findDir(lessonDir)
    invokeAndWaitIfNeeded { runWriteAction { taskDir?.delete(studentTask) } }
  }

  @Throws(IOException::class)
  private fun createTaskDirectories(project: Project, lessonDir: VirtualFile, task: Task) {
    if (!task.lesson.course.isStudy) {
      CCUtils.initializeTaskPlaceholders(project.toCourseInfoHolder(), task)
    }
    GeneratorUtils.createTask(project, task, lessonDir)
  }

  private fun deleteRemovedItems(remoteItemsIds: List<Int>, items: List<StudyItem>) {
    val itemsToDelete = items.filter { it.id !in remoteItemsIds }
    if (itemsToDelete.isNotEmpty()) {
      runInEdt {
        runWriteAction {
          for (item in itemsToDelete) {
            val virtualFile = item.getDir(project.courseDir)
            CommandProcessor.getInstance().executeCommand(project, {
              virtualFile?.delete(CCStudyItemDeleteProvider::class.java)
            }, "", this.javaClass)
          }
        }
      }
    }
  }

  private fun constructDir(newItem: StudyItem, currentItem: StudyItem): VirtualFile {
    if (!renamed(currentItem, newItem)) {
      return currentItem.getDir(project.courseDir) ?: error("Dir for $currentItem is null")
    }

    val itemDir = newItem.getDir(project.courseDir)
    if (itemDir != null) {
      saveExistingDirectory(itemDir, newItem)
    }

    val currentItemDir = getCurrentItemDir(currentItem)
    rename(currentItemDir, newItem.name)

    return currentItemDir
  }

  private fun getCurrentItemDir(item: StudyItem): VirtualFile {
    val dirsMap = if (item is Section) oldSectionDirectories else oldLessonDirectories
    val id = item.id

    val savedDir = dirsMap[id]
    return if (savedDir != null) {
      dirsMap.remove(id)
      savedDir
    }
    else {
      val oldItemDirectory = item.getDir(project.courseDir) ?: error("Dir is null for $item")
      oldItemDirectory
    }
  }

  private fun saveExistingDirectory(itemDir: VirtualFile, item: StudyItem) {
    val directoryMap = if (item is Lesson) oldLessonDirectories else oldSectionDirectories

    val id = (item as? Lesson)?.id ?: (item as? Section)?.id ?: return
    invokeAndWaitIfNeeded {
      runWriteAction {
        try {
          itemDir.rename(item, "old_${itemDir.name}")
          directoryMap[id] = itemDir
        }
        catch (e: IOException) {
          LOG.warn(e.message)
        }
      }
    }
  }

  private fun renamed(currentItem: StudyItem, newItem: StudyItem) = currentItem.name != newItem.name

  private fun rename(dirToRename: VirtualFile, s: String) {
    invokeAndWaitIfNeeded {
      runWriteAction {
        try {
          dirToRename.rename(this, s)
        }
        catch (e: IOException) {
          LOG.warn(e)
        }
      }
    }
  }

  companion object {
    private val LOG = logger<EduCourseUpdater>()
  }
}