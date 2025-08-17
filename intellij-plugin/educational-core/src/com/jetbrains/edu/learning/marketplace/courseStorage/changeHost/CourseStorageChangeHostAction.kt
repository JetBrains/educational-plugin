package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class CourseStorageChangeHostAction : ChangeServiceHostAction<CourseStorageServiceHost>(CourseStorageServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.CourseStorageChangeHost"
  }
}
