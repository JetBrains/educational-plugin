package com.jetbrains.edu.learning.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.EduTaskChecker;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.CheckUtils;
import com.jetbrains.edu.learning.checker.TestsOutputParser;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;


public abstract class RunConfigurationBasedTaskChecker extends EduTaskChecker {
  public static final String TEST_RUNNER_CLASS = "EduTestRunner";
  private static final Logger LOG = Logger.getInstance(RunConfigurationBasedTaskChecker.class);

  public RunConfigurationBasedTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project);
  }

  @Override
  public void clearState() {
    CheckUtils.drawAllPlaceholders(myProject, myTask);
  }

  @Override
  public CheckResult check() {
    Ref<CheckResult> result = new Ref<>(new CheckResult(CheckStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH));
    Sdk sdk = ProjectRootManager.getInstance(myProject).getProjectSdk();
    if (sdk == null) {
      return result.get();
    }
    final VirtualFile testsFile = getTestsFile();
    if (testsFile == null) {
      return result.get();
    }
    VirtualFile taskDir = myTask.getTaskDir(myProject);
    if (taskDir == null) {
      return result.get();
    }
    Module module = ModuleUtilCore.findModuleForFile(taskDir, myProject);
    if (module == null) {
      return result.get();
    }
    CountDownLatch latch = new CountDownLatch(1);
    ApplicationManager.getApplication().invokeAndWait(() -> CompilerManager.getInstance(myProject).make(module, (aborted, errors, warnings, compileContext) -> {
      if (errors != 0) {
        result.set(new CheckResult(CheckStatus.Unchecked, "Code has compilation errors"));
        latch.countDown();
        return;
      }
      if (aborted) {
        result.set(new CheckResult(CheckStatus.Unchecked, "Compilation aborted"));
        latch.countDown();
        return;
      }
      RunnerAndConfigurationSettings javaTemplateConfiguration = produceRunConfiguration(myProject,
        "javaTemplateConfiguration", ApplicationConfigurationType.getInstance());

      setProcessParameters(myProject,
        ((ApplicationConfiguration) javaTemplateConfiguration.getConfiguration()),module, testsFile);

      RunProfileState state = getState(javaTemplateConfiguration);

      if (state == null) {
        //exception is logged inside getState method
        latch.countDown();
        return;
      }

      final JavaCommandLineState javaCmdLine = (JavaCommandLineState) state;
      ApplicationManager.getApplication().invokeLater(() -> FileDocumentManager.getInstance().saveAllDocuments());
      DumbService.getInstance(myProject).runWhenSmart(() -> {
        try {
          JavaParameters javaParameters;
          javaParameters = javaCmdLine.getJavaParameters();
          GeneralCommandLine fromJavaParameters = CommandLineBuilder.createFromJavaParameters(javaParameters, myProject, false);
          Process process = fromJavaParameters.createProcess();
          TestsOutputParser.TestsOutput output =
            CheckUtils
              .getTestOutput(process, fromJavaParameters.getCommandLineString(), myTask.getLesson().getCourse().isAdaptive());
          result.set(new CheckResult(output.isSuccess() ? CheckStatus.Solved : CheckStatus.Failed, output.getMessage()));
        } catch (ExecutionException e) {
          LOG.error(e);
        }
        finally {
          latch.countDown();
        }
      });
    }));
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOG.error(e);
    }
    return result.get();
  }

  @Nullable
  protected abstract VirtualFile getTestsFile();

  @Nullable
  private RunProfileState getState(RunnerAndConfigurationSettings javaTemplateConfiguration) {
    try {
      return javaTemplateConfiguration.getConfiguration().
        getState(DefaultRunExecutor.getRunExecutorInstance(),
          ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(),
            javaTemplateConfiguration).build());
    } catch (ExecutionException e) {
      LOG.error(e);
      return null;
    }
  }

  @NotNull
  private RunnerAndConfigurationSettings produceRunConfiguration(Project project, String name, ConfigurationType type) {
    return RunManager.getInstance(project).createRunConfiguration(name, type.getConfigurationFactories()[0]);
  }

  protected abstract void setProcessParameters(Project project, ApplicationConfiguration configuration,
                                               Module module, @NotNull VirtualFile testsFile);
}
