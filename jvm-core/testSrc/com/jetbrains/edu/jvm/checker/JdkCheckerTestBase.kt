package com.jetbrains.edu.jvm.checker

import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.IdeaTestUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.checker.CheckersTestBase
import org.junit.Assert
import java.io.File

abstract class JdkCheckerTestBase : CheckersTestBase<JdkProjectSettings>() {

  override val projectSettings: JdkProjectSettings get() {
    val jdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME)
              ?: error("Gradle JDK should be configured in setUp()")

    val sdksModel = ProjectSdksModel()
    sdksModel.addSdk(jdk)

    return JdkProjectSettings(sdksModel, object : JdkComboBox.JdkComboBoxItem() {
      override fun getJdk() = jdk
      override fun getSdkName() = jdk.name
    })
  }

  override fun setUpEnvironment() {
    val myJdkHome = IdeaTestUtil.requireRealJdkHome()
    VfsRootAccess.allowRootAccess(testRootDisposable, myJdkHome)

    object : WriteAction<Any>() {
      override fun run(result: Result<Any>) {
        val oldJdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME)
        if (oldJdk != null) {
          ProjectJdkTable.getInstance().removeJdk(oldJdk)
        }
        val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(myJdkHome))!!
        val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, MY_TEST_JDK_NAME)
        Assert.assertNotNull("Cannot create JDK for $myJdkHome", jdk)
        ProjectJdkTable.getInstance().addJdk(jdk!!)
      }
    }.execute()
  }

  override fun tearDownEnvironment() {
    object : WriteAction<Any>() {
      override fun run(result: Result<Any>) {
        val old = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME)
        if (old != null) {
          SdkConfigurationUtil.removeSdk(old)
        }
      }
    }.execute()
  }

  companion object {
    private const val MY_TEST_JDK_NAME = "Test JDK"
  }
}
