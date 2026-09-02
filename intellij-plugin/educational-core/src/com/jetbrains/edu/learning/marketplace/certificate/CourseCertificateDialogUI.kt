package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.Course
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.TestOnly

interface CourseCertificateDialogUI {
  fun show()
}

@Service(Service.Level.PROJECT)
@State(name = "CourseCertificateDialog", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CourseCertificateDialogUIFactory(private val project: Project) :
  SerializablePersistentStateComponent<CourseCertificateDialogUIFactory.DialogState>(DialogState()),
  EduTestAware {

  val isDismissed: Boolean
    get() = state.isDismissed

  fun create(course: Course, certificateId: String?): CourseCertificateDialogUI =
    CourseCertificateDialog(project, course.name, certificateId)

  fun dismiss() {
    updateState { it.copy(isDismissed = true) }
  }

  @TestOnly
  override fun cleanUpState() {
    updateState { DialogState() }
  }

  @Serializable
  data class DialogState(val isDismissed: Boolean = false)

  companion object {
    fun getInstance(project: Project): CourseCertificateDialogUIFactory = project.service()
  }
}
