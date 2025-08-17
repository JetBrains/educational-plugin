package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.jetbrains.edu.learning.actions.changeHost.ChangeServiceHostActionTestBase
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager.SelectedServiceHost
import org.junit.runners.Parameterized

class CourseStorageChangeHostActionTest(
  initialValue: SelectedServiceHost<CourseStorageServiceHost>?,
  dialogValue: SelectedServiceHost<CourseStorageServiceHost>?,
  expectedValue: SelectedServiceHost<CourseStorageServiceHost>
) : ChangeServiceHostActionTestBase<CourseStorageServiceHost>(initialValue, dialogValue, expectedValue) {

  override val manager: ServiceHostManager<CourseStorageServiceHost> = CourseStorageServiceHost
  override val actionId: String = CourseStorageChangeHostAction.ACTION_ID

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0} -> {1}")
    fun data(): Collection<Array<Any?>> = CourseStorageServiceHost.data()
  }
}
