package com.jetbrains.edu.jvm.checker

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.IdeaTestUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import org.junit.Assert
import java.io.File

class JdkCheckerFixture : EduCheckerFixture<JdkProjectSettings>() {

  override val projectSettings: JdkProjectSettings get() {
    val jdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME) ?: error("Gradle JDK should be configured in setUp()")

    val sdksModel = ProjectSdksModel()
    sdksModel.addSdk(jdk)

    return JdkProjectSettings(sdksModel, object : JdkComboBox.JdkComboBoxItem() {
      override fun getJdk() = jdk
      override fun getSdkName() = jdk.name
    })
  }

  override fun getSkipTestReason(): String? {
    // We temporarily disable checkers tests on teamcity linux agents
    // because they don't work on these agents and we can't find out a reason :((
    return if (SystemInfo.isLinux && System.getenv("TEAMCITY_VERSION") == null) "Linux TeamCity agent" else super.getSkipTestReason()
  }

  override fun setUp() {
    val myJdkHome = IdeaTestUtil.requireRealJdkHome()
    VfsRootAccess.allowRootAccess(testRootDisposable, myJdkHome)

    val oldJdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME)
    if (oldJdk != null) {
      ProjectJdkTable.getInstance().removeJdk(oldJdk)
    }
    val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(myJdkHome))!!
    val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, MY_TEST_JDK_NAME)
    Assert.assertNotNull("Cannot create JDK for $myJdkHome", jdk)
    SdkConfigurationUtil.addSdk(jdk!!)
  }

  override fun tearDown() {
    val old = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME) ?: return
    SdkConfigurationUtil.removeSdk(old)
  }

  companion object {
    private const val MY_TEST_JDK_NAME = "Test JDK"
  }
}
