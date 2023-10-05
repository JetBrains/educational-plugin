package com.jetbrains.edu.remote

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.APP)
class EduRemoteUidRetrieverService(private val scope: CoroutineScope) {
  private val checkInterval = 5.seconds
  private val isStarted = AtomicBoolean(false)
  private val filePath = System.getProperty(INFO_FILE_PATH_PROPERTY_NAME)?.toString() ?: INFO_FILE_PATH

  fun start() {
    if (!isStarted.compareAndSet(false, true)) {
      error("There was an attempt to start the service again, while it is already running")
    }
    LOG.debug("EduRemoteUidRetrieverService started")
    scope.launch {
      while (true) {
        val fileWithUid = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
        if (fileWithUid != null) {
          val fileText = fileWithUid.readText()
          val matcher = PATTERN.matcher(fileText)

          if (matcher.find()) {
            val uid = matcher.group(1)
            if (uid matches UID_REGEX) {
              PropertiesComponent.getInstance().setValue(REMOTE_DEV_USER_UID_PROPERTY, uid)
              LOG.info("The user's UID ($uid) was successfully retrieved and stored")
            }
            else {
              LOG.error("Provided UID is invalid: $uid")
            }
            break
          }
          else {
            LOG.warn(
              "The user UID file ($filePath) was located in the file system, however, " +
              "it does not contain any user UID information. Re-checking after an interval of $checkInterval"
            )
          }
        }
        else {
          LOG.warn("The user UID file ($filePath) is not found in the file system. Will re-check after a delay of $checkInterval")
        }
        delay(checkInterval)
      }
    }
  }

  companion object {
    @NonNls
    private const val INFO_FILE_PATH: String = "/idea/podinfo/labels"

    @NonNls
    private const val INFO_FILE_PATH_PROPERTY_NAME: String = "edu.remote.ide.info.file.path"

    @NonNls
    private const val JB_UID_PROPERTY_NAME_IN_FILE: String = "jbUid"

    private val LOG = thisLogger()
    private val PATTERN = "$JB_UID_PROPERTY_NAME_IN_FILE=(\\S+)".toPattern()

    /**
     * The user UID can range from 22 to 25 characters, but it might also be longer
     */
    private val UID_REGEX = "^[a-z0-9]{22,}$".toRegex()
  }

}