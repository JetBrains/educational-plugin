package com.jetbrains.edu.kotlin.android;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.kotlin.EduKotlinPluginConfigurator;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.checker.StudyCheckResult;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.checker.StudyTestsOutputParser;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.stepic.StepicUser;
import com.jetbrains.edu.utils.EduIntellijUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
      public StudyCheckResult check() {
        GeneralCommandLine cmd = new GeneralCommandLine();
        String testsPath = FileUtil.join(FileUtil.toSystemDependentName(project.getBaseDir().getPath()), "edu-tests");
        cmd.withWorkDirectory(testsPath);
        cmd.setExePath(GRADLEW);
        cmd.addParameter(CONNECTED_ANDROID_TEST);
        try {
          return getTestOutput(cmd.createProcess(), cmd.getCommandLineString());
        } catch (ExecutionException e) {
          return new StudyCheckResult(StudyStatus.Unchecked, StudyCheckAction.FAILED_CHECK_LAUNCH);
        }
      }

      private StudyCheckResult getTestOutput(@NotNull Process testProcess,
                                             @NotNull String commandLine) {
        final CapturingProcessHandler handler = new CapturingProcessHandler(testProcess, null, commandLine);
        final ProcessOutput output = ProgressManager.getInstance().hasProgressIndicator() ? handler
          .runProcessWithProgressIndicator(ProgressManager.getInstance().getProgressIndicator()) :
          handler.runProcess();
        List<String> stdoutLines = output.getStdoutLines();
        for (String line : stdoutLines) {
          if (line.startsWith(BUILD_SUCCESSFUL)) {
            return new StudyCheckResult(StudyStatus.Solved, StudyTestsOutputParser.CONGRATULATIONS);
          }
        }
        // TODO: informative error message
        return new StudyCheckResult(StudyStatus.Failed, "Test Failed");
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
}
