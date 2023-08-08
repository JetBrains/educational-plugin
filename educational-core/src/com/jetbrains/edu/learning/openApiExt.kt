package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.fileTypes.impl.DetectedByContentFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.BroadcastDirection
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.isBinary
import com.jetbrains.edu.learning.courseFormat.mimeFileType
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.VisibleForTesting
import java.util.*

private val LOG = Logger.getInstance("openApiExt")

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment

//Extension for binary files to mark files that are needed to be encoded
//In test environment most of binary file extensions are recognized as unknown
//because the plugins for their support are not available
@VisibleForTesting
const val EDU_TEST_BIN = "edutestbin"

fun checkIsBackgroundThread() {
  check(!ApplicationManager.getApplication().isDispatchThread) {
    "Long running operation invoked on UI thread"
  }
}

fun checkIsWriteActionAllowed() {
  check(ApplicationManager.getApplication().isWriteAccessAllowed) {
    "Write action is required"
  }
}

/**
 * Invokes [runnable] asynchronously in EDT checking that [Project] is not disposed yet
 *
 * @see com.intellij.openapi.application.Application.invokeLater
 */
inline fun Project.invokeLater(modalityState: ModalityState? = null, crossinline runnable: () -> Unit) {
  if (modalityState == null) {
    ApplicationManager.getApplication().invokeLater({ runnable() }, disposed)
  }
  else {
    ApplicationManager.getApplication().invokeLater({ runnable() }, modalityState, disposed)
  }
}

/**
 * Note: there are some unsupported cases in this method.
 * For example, some files have known file type but no extension
 */
fun toEncodeFileContent(virtualFile: VirtualFile): Boolean {
  val path = virtualFile.path
  val name = PathUtil.getFileName(path)
  val extension = FileUtilRt.getExtension(name)
  if (isUnitTestMode && extension == EDU_TEST_BIN) {
    return true
  }
  val fileType = FileTypeManagerEx.getInstance().getFileTypeByFile(virtualFile)
  if (fileType !is UnknownFileType && fileType !is DetectedByContentFileType) {
    return fileType.isBinary
  }
  if (fileType is DetectedByContentFileType && extension == "db") {
    /** We do encode *.db files when sending them to Stepik. When we get them back they have [DetectedByContentFileType] fileType and by
     * default this file type is not binary, so we have to forcely specify it as binary
     */
    return true
  }
  val contentType = mimeFileType(path) ?: return isGitObject(name)
  return isBinary(contentType)
}

private fun isGitObject(name: String): Boolean {
  return (name.length == 38 || name.length == 40) && name.matches(Regex("[a-z0-9]+"))
}

val Project.courseDir: VirtualFile
  get() {
    return guessCourseDir() ?: error("Failed to find course dir for $this")
  }

fun Project.guessCourseDir(): VirtualFile? {
  val projectDir = guessProjectDir() ?: return null
  return if (projectDir.name == Project.DIRECTORY_STORE_FOLDER) {
    projectDir.parent
  }
  else projectDir
}

val Project.selectedEditor: Editor? get() = selectedVirtualFile?.getEditor(this)

val Project.selectedVirtualFile: VirtualFile? get() = FileEditorManager.getInstance(this)?.selectedFiles?.firstOrNull()

val Project.selectedTaskFile: TaskFile? get() = selectedVirtualFile?.getTaskFile(this)

val AnActionEvent.eduState: EduState?
  get() {
    val project = getData(CommonDataKeys.PROJECT) ?: return null
    val editor = getData(CommonDataKeys.HOST_EDITOR) ?: return null
    val virtualFile = getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    val taskFile = virtualFile.getTaskFile(project) ?: return null
    return EduState(project, virtualFile, editor, taskFile)
  }

val Project.eduState: EduState?
  get() {
    val virtualFile = selectedVirtualFile ?: return null
    val taskFile = virtualFile.getTaskFile(this) ?: return null
    val editor = virtualFile.getEditor(this) ?: return null
    return EduState(this, virtualFile, editor, taskFile)
  }

val Project.course: Course? get() = StudyTaskManager.getInstance(this).course

val String.xmlEscaped: String get() = StringUtil.escapeXmlEntities(this)

val String.xmlUnescaped: String get() = StringUtil.unescapeXmlEntities(this)

inline fun <T> runReadActionInSmartMode(project: Project, crossinline runnable: () -> T): T {
  return DumbService.getInstance(project).runReadActionInSmartMode(Computable { runnable() })
}

fun Document.toPsiFile(project: Project): PsiFile? {
  return PsiDocumentManager.getInstance(project).getPsiFile(this)
}

fun <T> computeUnderProgress(
  project: Project? = null,
  title: String,
  canBeCancelled: Boolean = true,
  computation: (ProgressIndicator) -> T
): T =
  ProgressManager.getInstance().run(object : Task.WithResult<T, Exception>(project, title, canBeCancelled) {
    override fun compute(indicator: ProgressIndicator): T {
      return computation(indicator)
    }
  })

fun runInBackground(project: Project? = null, title: String, canBeCancelled: Boolean = true, task: (ProgressIndicator) -> Unit) =
  ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, canBeCancelled) {
    override fun run(indicator: ProgressIndicator) = task(indicator)
  })

fun <T> withRegistryKeyOff(key: String, action: () -> T): T {
  val registryValue = Registry.get(key)
  val before = try {
    registryValue.asBoolean()
  }
  catch (e: MissingResourceException) {
    LOG.error(e)
    return action()
  }

  try {
    registryValue.setValue(false)
    return action()
  }
  finally {
    registryValue.setValue(before)
  }
}

fun <V> getInEdt(modalityState: ModalityState = ModalityState.defaultModalityState(), compute: () -> V): V {
  return runBlocking(AppUIExecutor.onUiThread(modalityState).coroutineDispatchingContext()) {
    compute()
  }
}

inline fun <reified L> createTopic(
  displayName: String,
  direction: BroadcastDirection = BroadcastDirection.TO_CHILDREN
): Topic<L> = Topic.create(displayName, L::class.java, direction)
