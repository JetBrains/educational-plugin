package com.jetbrains.edu.learning.storage

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.LightTestAware
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isLight
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.yaml.format.student.TakeFromStorageBinaryContents
import com.jetbrains.edu.learning.yaml.format.student.TakeFromStorageTextualContents
import org.jetbrains.annotations.TestOnly

@Service(Service.Level.PROJECT)
class LearningObjectsStorageManager(private val project: Project) : DumbAware, Disposable, LightTestAware {

  /**
   * This is the project level storage used to store all the edu files contents and other data that should be persistent.
   */
  private val learningObjectsStorage: LearningObjectsStorage = createLearningObjectStorage()

  val writeTextInYaml: Boolean get() = learningObjectsStorage.writeTextInYaml

  init {
    Disposer.register(this, learningObjectsStorage)
  }

  private fun EduFile.persist(storage: LearningObjectsStorage) {
    when (contents) {
      is ContentsFromLearningObjectsStorage -> {
        return
      }
      TakeFromStorageBinaryContents -> {
        contents = BinaryContentsFromLearningObjectsStorage(storage, pathInStorage)
      }
      TakeFromStorageTextualContents -> {
        contents = TextualContentsFromLearningObjectsStorage(storage, pathInStorage)
      }
      else -> {
        val initialContents = contents
        val contentsWithDiagnostics = wrapWithDiagnostics(initialContents, pathInStorage)

        // this will allow logging all accesses to the contents while it is being persisted
        contents = contentsWithDiagnostics
        ApplicationManager.getApplication().executeOnPooledThread {
          val persistedContents = try {
            initialContents.persist(storage, pathInStorage)
          }
          catch(e: Exception) {
            logger<LearningObjectsStorageManager>().error("Exception during persisting contents for EduFile $pathInStorage", e)
            initialContents
          }
          // if persisting took long, contents could have been already changed

          if (!setContentsIfEquals(contentsWithDiagnostics, persistedContents)) {
            // The level is `error`, because we want to have feedback if that happens.
            // But we will probably change this later to `warn`, or 'info', or remove the check completely.
            logger<FileContents>().error("Contents of a file changed while the file was being persisted: $pathInStorage from ${initialContents.javaClass} to ${contents.javaClass}")
          }
        }
      }
    }
  }

  private fun FileContents.persist(storage: LearningObjectsStorage, path: String): FileContents {
    return when (this) {
      is BinaryContents -> {
        storage.store(path, bytes)
        BinaryContentsFromLearningObjectsStorage(storage, path)
      }

      is TextualContents -> {
        storage.store(path, text)
        TextualContentsFromLearningObjectsStorage(storage, path)
      }

      is UndeterminedContents -> {
        storage.store(path, textualRepresentation)
        UndeterminedContentsFromLearningObjectsStorage(storage, path)
      }
    }
  }

  fun persistAllEduFiles(course: Course) {
    if (project.course?.isStudy != true) return

    course.visitEduFiles { eduFile ->
      eduFile.persist(learningObjectsStorage)
    }
  }

  fun persistTaskEduFiles(task: Task) {
    if (project.course?.isStudy != true) return

    for (taskFile in task.taskFiles.values) {
      taskFile.persist(learningObjectsStorage)
    }
  }

  /**
   * Takes the learning objects storage type from the project settings, or, if nothing is configured,
   * updates settings so that they have the default storage type taken from the Registry.
   */
  private fun createLearningObjectStorage(): LearningObjectsStorage {
    val propertiesComponent = PropertiesComponent.getInstance(project)
    val storageValue = propertiesComponent.getValue(PROPERTIES_KEY)

    val storage = createStorageByType(LearningObjectStorageType.safeValueOf(storageValue), project)

    if (storage != null) {
      return storage
    }

    @Suppress("TestOnlyProblems")
    val defaultStorageType = if (isUnitTestMode && project.isLight || project.isDefault) {
      LearningObjectStorageType.InMemory
    }
    else {
      getDefaultLearningObjectsStorageType()
    }
    propertiesComponent.setValue(PROPERTIES_KEY, defaultStorageType.toString())
    return createStorageByType(defaultStorageType, project) ?: YamlLearningObjectsStorage()
  }

  private fun createStorageByType(type: LearningObjectStorageType?, project: Project): LearningObjectsStorage? = when (type) {
    LearningObjectStorageType.YAML -> YamlLearningObjectsStorage()
    LearningObjectStorageType.InMemory -> InMemoryLearningObjectsStorage()
    LearningObjectStorageType.SQLite -> SQLiteLearningObjectsStorage.openOrCreateDB(project)
    else -> null
  }

  override fun dispose() {}

  @TestOnly
  override fun cleanUpState() {
    (learningObjectsStorage as? InMemoryLearningObjectsStorage)?.clear()
  }

  companion object {
    fun getInstance(project: Project): LearningObjectsStorageManager = project.service()

    private const val PROPERTIES_KEY = "Edu.LearningObjectsStorageType"
  }
}

fun Task.persistEduFiles(project: Project) {
  LearningObjectsStorageManager.getInstance(project).persistTaskEduFiles(this)
}

val EduFile.pathInStorage: String
  get() = if (this is TaskFile) {
    task.pathInCourse + "/"
  }
  else {
    ""
  } + name