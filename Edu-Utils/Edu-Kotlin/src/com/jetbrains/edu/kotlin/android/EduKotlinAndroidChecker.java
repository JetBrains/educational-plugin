package com.jetbrains.edu.kotlin.android;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.tools.idea.avdmanager.AvdManagerConnection;
import com.android.tools.idea.avdmanager.AvdOptionsModel;
import com.android.tools.idea.avdmanager.AvdWizardUtils;
import com.android.tools.idea.ddms.adb.AdbService;
import com.android.tools.idea.run.LaunchableAndroidDevice;
import com.android.tools.idea.wizard.model.ModelWizardDialog;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.checker.StudyCheckResult;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.checker.StudyTestsOutputParser;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.stepic.StepicUser;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


class EduKotlinAndroidChecker extends StudyTaskChecker<PyCharmTask> {
  private static final String BUILD_SUCCESSFUL = "BUILD SUCCESSFUL";
  private static final String GRADLEW = "../gradlew";
  private static final String CONNECTED_ANDROID_TEST = "connectedAndroidTest";

  public EduKotlinAndroidChecker(@NotNull PyCharmTask task, @NotNull Project project) {
    super(task, project);
  }

  @Override
  public boolean validateEnvironment() {
    try {
      if (startEmulatorIfExists()) {
        return true;
      }
      Messages.showInfoMessage(myProject, "Android emulator is required to check tasks. New emulator will be created and launched. ", "Android Emulator not Found");
      AvdOptionsModel avdOptionsModel = new AvdOptionsModel(null);
      ModelWizardDialog dialog = AvdWizardUtils.createAvdWizard(null, myProject, avdOptionsModel);
      if (dialog.showAndGet()) {
        AvdInfo avd = avdOptionsModel.getCreatedAvd();
        return launchEmulator(avd);
      }
    } catch (Exception e) {
      // ignore
    }
    return false;
  }

  @Override
  public StudyCheckResult check() {
    try {
      GeneralCommandLine cmd = new GeneralCommandLine();
      String testsPath = FileUtil.join(FileUtil.toSystemDependentName(myProject.getBaseDir().getPath()), "edu-tests");
      cmd.withWorkDirectory(testsPath);
      cmd.setExePath(GRADLEW);
      cmd.addParameter(CONNECTED_ANDROID_TEST);
      return getTestOutput(cmd.createProcess(), cmd.getCommandLineString());
    } catch (Exception e) {
      return new StudyCheckResult(StudyStatus.Unchecked, StudyCheckAction.FAILED_CHECK_LAUNCH);
    }
  }

  private boolean startEmulatorIfExists() throws Exception {
    AndroidDebugBridge bridge = AdbService.getInstance().getDebugBridge(AndroidSdkUtils.getAdb(myProject)).get();
    IDevice[] devices = bridge.getDevices();
    for (IDevice device : devices) {
      if (device.isEmulator() && device.getAvdName() != null) {
        return true; // there is running emulator
      }
    }

    for (AvdInfo avd : AvdManagerConnection.getDefaultAvdManagerConnection().getAvds(true)) {
      if (launchEmulator(avd)) {
        return true;
      }
    }
    return false;
  }

  private boolean launchEmulator(@NotNull AvdInfo avd) throws Exception {
    if (avd.getStatus().equals(AvdInfo.AvdStatus.OK)) {
      LaunchableAndroidDevice launchableAndroidDevice = new LaunchableAndroidDevice(avd);
      IDevice device = ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.WithResult<IDevice, Exception>(myProject, "Launching Emulator", false) {
        ListenableFuture<IDevice> future;

        @Override
        protected IDevice compute(@NotNull ProgressIndicator indicator) throws Exception {
          indicator.setIndeterminate(true);
          ApplicationManager.getApplication().invokeAndWait(() -> future = launchableAndroidDevice.launch(myProject));
          return future.get();
        }

        @Override
        public void onCancel() {
          if (future != null) {
            future.cancel(true);
          }
        }
      });
      if (device != null) {
        return true;
      }
    }
    return false;
  }

  private StudyCheckResult getTestOutput(@NotNull Process testProcess,
                                         @NotNull String commandLine) {
    final CapturingProcessHandler handler = new CapturingProcessHandler(testProcess, null, commandLine);
    final ProcessOutput output = ProgressManager.getInstance().hasProgressIndicator() ? handler
      .runProcessWithProgressIndicator(ProgressManager.getInstance().getProgressIndicator()) :
      handler.runProcess();
    List<String> stdoutLines = output.getStdoutLines();
    boolean buildSuccessful = false;
    for (String line : stdoutLines) {
      if (line.startsWith(BUILD_SUCCESSFUL)) {
        buildSuccessful = true;
      }
    }
    final StudyTestsOutputParser.TestsOutput testsOutput = StudyTestsOutputParser.getTestsOutput(output, false);
    if (testsOutput.isSuccess() && !buildSuccessful) {
      return new StudyCheckResult(StudyStatus.Unchecked, StudyCheckAction.FAILED_CHECK_LAUNCH);
    }
    return new StudyCheckResult(testsOutput.isSuccess() ? StudyStatus.Solved : StudyStatus.Failed, testsOutput.getMessage());
  }

  @Override
  public StudyCheckResult checkOnRemote(@Nullable StepicUser user) {
    return super.checkOnRemote(user);
  }

  @Override
  public void clearState() {
    super.clearState();
  }
}
