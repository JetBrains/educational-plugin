package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.remoteConfigFileName
import com.jetbrains.edu.learning.yaml.YamlMapper.basicMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.remoteMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.studentMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.studentMapperWithEncryption
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

object YamlFormatSynchronizer {
  val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")

  @Deprecated("Use `saveAllSync` instead")
  fun saveAll(project: Project) {
    val course = project.course ?: error("Attempt to create config files for project without course")
    val items = course.allSubitems()
    val mapper = course.mapper()
    for (item in items) {
      saveItem(item, mapper)
    }

    saveRemoteInfo(course)
  }

  suspend fun saveAllSync(project: Project) {
    val course = project.course ?: error("Attempt to create config files for project without course")
    val items = course.allSubitems()
    val mapper = course.mapper()
    for (item in items) {
      saveItemSync(item, mapper)
    }

    saveRemoteInfoSync(course)
  }

  @Deprecated("Use `saveItemSync` instead")
  fun saveItem(item: StudyItem, mapper: ObjectMapper = item.course.mapper(), configName: String = item.configFileName) {
    val project = getProjectIfConfigFilesEnabled(item) ?: return
    YamlConfigSyncService.getInstance(project).save(item, configName, mapper)
  }

  suspend fun saveItemSync(item: StudyItem, mapper: ObjectMapper = item.course.mapper(), configName: String = item.configFileName) {
    val project = getProjectIfConfigFilesEnabled(item) ?: return
    YamlConfigSyncService.getInstance(project).saveSync(item, configName, mapper)
  }

  private fun getProjectIfConfigFilesEnabled(item: StudyItem): Project? {
    val course = item.course

    @NonNls
    val errorMessageToLog = "Failed to find project for course"
    val project = course.project ?: error(errorMessageToLog)
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) {
      return null
    }
    return project
  }

  @Deprecated("Use `saveRemoteInfoSync` instead")
  fun saveRemoteInfo(item: StudyItem) {
    for (itemWithRemoteInfo in item.allSubitems()) {
      saveItemRemoteInfo(itemWithRemoteInfo)
    }
  }

  suspend fun saveRemoteInfoSync(item: StudyItem) {
    for (itemWithRemoteInfo in item.allSubitems()) {
      saveItemRemoteInfoSync(itemWithRemoteInfo)
    }
  }

  @Deprecated("Use `saveItemWithRemoteInfoSync` instead")
  fun saveItemWithRemoteInfo(item: StudyItem) {
    saveItem(item)
    saveRemoteInfo(item)
  }

  suspend fun saveItemWithRemoteInfoSync(item: StudyItem) {
    saveItemSync(item)
    saveRemoteInfoSync(item)
  }

  @Deprecated("Use `saveItemRemoteInfoSync` instead")
  private fun saveItemRemoteInfo(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (shouldSaveRemoteInfo(item)) {
      saveItem(item, remoteMapper(), item.remoteConfigFileName)
    }
  }

  private suspend fun saveItemRemoteInfoSync(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (shouldSaveRemoteInfo(item)) {
      saveItemSync(item, remoteMapper(), item.remoteConfigFileName)
    }
  }

  private fun shouldSaveRemoteInfo(item: StudyItem): Boolean = item.id > 0

  private fun StudyItem.allSubitems(): List<StudyItem> {
    val result = mutableListOf<StudyItem>()

    fun collect(item: StudyItem) {
      result.add(item)
      if (item is ItemContainer) {
        item.items.forEach { collect(it) }
      }
    }

    collect(this)
    return result
  }

  fun startSynchronization(project: Project) {
    if (isUnitTestMode) {
      return
    }

    val disposable = StudyTaskManager.getInstance(project)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), disposable)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (isLocalConfigFile(file)) {
          if (project.isStudentProject()) {
            @NonNls
            val errorMessageToLog = "Can't find editor for a file: ${file.name}"
            val editor = file.getEditor(project) ?: error(errorMessageToLog)
            showNoEditingNotification(editor)
            return
          }

          // load item to show editor notification if config file is invalid
          YamlLoader.loadItem(project, file, false)
        }
      }
    })
  }

  private fun showNoEditingNotification(editor: Editor) {
    val label = JLabel(EduCoreBundle.message("yaml.editor.notification.configuration.file"))
    label.border = JBUI.Borders.empty(5, 10, 5, 0)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.background = MessageType.WARNING.popupBackground

    editor.headerComponent = panel
  }

  fun isConfigFile(file: VirtualFile): Boolean {
    return isLocalConfigFile(file) || isRemoteConfigFile(file)
  }

  fun isRemoteConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return isRemoteConfigFileName(name)
  }

  fun isRemoteConfigFileName(name: String): Boolean {
    return REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }

  fun isLocalConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return isLocalConfigFileName(name)
  }

  fun isLocalConfigFileName(name: String): Boolean {
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  fun Course.mapper(): ObjectMapper = if (isStudy) {
    if (isMarketplace) studentMapperWithEncryption() else studentMapper()
  }
  else {
    basicMapper()
  }
}