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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.kotlin.EduKotlinPluginConfigurator;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.checker.StudyCheckResult;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.checker.StudyTestsOutputParser;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import com.jetbrains.edu.learning.stepic.StepicUser;
import com.jetbrains.edu.utils.EduIntellijUtils;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import com.jetbrains.edu.utils.EduPluginConfiguratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EduKotlinAndroidPluginConfigurator extends EduKotlinPluginConfigurator {

  private static final String BUILD_SUCCESSFUL = "BUILD SUCCESSFUL";
  private static final String GRADLEW = "../gradlew";
  private static final String CONNECTED_ANDROID_TEST = "connectedAndroidTest";

  @NotNull
  @Override
  public StudyTaskChecker<PyCharmTask> getPyCharmTaskChecker(@NotNull PyCharmTask pyCharmTask, @NotNull Project project) {
    return new StudyTaskChecker<PyCharmTask>(pyCharmTask, project) {

      @Override
      public boolean validateEnvironment() {
        try {
          if (startEmulatorIfExists()) {
            return true;
          }
          Messages.showInfoMessage(project, "Android emulator is required to check tasks. New emulator will be created and launched. ", "Android Emulator not Found");
          AvdOptionsModel avdOptionsModel = new AvdOptionsModel(null);
          ModelWizardDialog dialog = AvdWizardUtils.createAvdWizard(null, project, avdOptionsModel);
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
          String testsPath = FileUtil.join(FileUtil.toSystemDependentName(project.getBaseDir().getPath()), "edu-tests");
          cmd.withWorkDirectory(testsPath);
          cmd.setExePath(GRADLEW);
          cmd.addParameter(CONNECTED_ANDROID_TEST);
          return getTestOutput(cmd.createProcess(), cmd.getCommandLineString());
        } catch (Exception e) {
          return new StudyCheckResult(StudyStatus.Unchecked, StudyCheckAction.FAILED_CHECK_LAUNCH);
        }
      }

      private boolean startEmulatorIfExists() throws Exception {
        AndroidDebugBridge bridge = AdbService.getInstance().getDebugBridge(AndroidSdkUtils.getAdb(project)).get();
        IDevice[] devices = bridge.getDevices();
        for (IDevice device : devices) {
          if (device.isEmulator() && device.getAvdName() != null) {
              return true; // there is running emulator
          }
        }

        for (AvdInfo avd : AvdManagerConnection.getDefaultAvdManagerConnection().getAvds(false)) {
          if (launchEmulator(avd)) {
            return true;
          }
        }
        return false;
      }

      private boolean launchEmulator(@NotNull AvdInfo avd) throws Exception {
        if (avd.getStatus().equals(AvdInfo.AvdStatus.OK)) {
          LaunchableAndroidDevice launchableAndroidDevice = new LaunchableAndroidDevice(avd);
          IDevice device = ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.WithResult<IDevice, Exception>(project, "Launching Emulator", false) {
              ListenableFuture<IDevice> future;

              @Override
              protected IDevice compute(@NotNull ProgressIndicator indicator) throws Exception {
                  indicator.setIndeterminate(true);
                  ApplicationManager.getApplication().invokeAndWait(() -> future = launchableAndroidDevice.launch(project));
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
    };
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = EduIntellijUtils.getBundledCourseRoot(KotlinAndroidCourseAction.DEFAULT_COURSE_PATH,
        KotlinAndroidCourseAction.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), KotlinAndroidCourseAction.DEFAULT_COURSE_PATH));
  }


  @Override
  public void createCourseModuleContent(@NotNull ModifiableModuleModel moduleModel,
                                        @NotNull Project project,
                                        @NotNull Course course,
                                        @Nullable String moduleDir) {
    final List<Lesson> lessons = course.getLessons();
    final Task task = lessons.get(0).getTaskList().get(0);

    if (moduleDir == null) {
      Logger.getInstance(EduPluginConfiguratorBase.class).error("Can't find module dir ");
      return;
    }

    final VirtualFile dir = VfsUtil.findFileByIoFile(new File(moduleDir), true);
    if (dir == null) {
      Logger.getInstance(EduPluginConfiguratorBase.class).error("Can't find module dir on file system " + moduleDir);
      return;
    }

    try {
      StudyGenerator.createTaskContent(task, dir);
    } catch (IOException e) {
      Logger.getInstance(EduPluginConfiguratorBase.class).error(e);
    }
  }

  @Override
  public EduCourseProjectGenerator getEduCourseProjectGenerator() {
    return new EduKotlinAndroidCourseProjectGenerator();
  }
}
