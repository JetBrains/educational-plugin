package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.ExceptionFilter;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.run.StudyExecutor;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.run.JetRunConfiguration;
import org.jetbrains.kotlin.idea.run.JetRunConfigurationType;

import java.util.List;


public class KotlinStudyExecutor implements StudyExecutor {
    private static final Logger LOG = Logger.getInstance(KotlinStudyExecutor.class);

    public Sdk findSdk(@NotNull final Project project) {
        return ProjectRootManager.getInstance(project).getProjectSdk();
    }

    @Override
    public StudyTestRunner getTestRunner(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
        return new KotlinStudyTestRunner(task, taskDir);
    }

    @Override
    public RunContentExecutor getExecutor(@NotNull final Project project, @NotNull final ProcessHandler handler) {
        return new RunContentExecutor(project, handler).withFilter(new ExceptionFilter(GlobalSearchScope.allScope(project)));
    }

    @Override
    public void setCommandLineParameters(@NotNull final GeneralCommandLine cmd,
                                         @NotNull final Project project,
                                         @NotNull final String filePath,
                                         @NotNull final String sdkPath,
                                         @NotNull final Task currentTask) {
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk != null) {
            RunnerAndConfigurationSettings temp = RunManager.getInstance(project).createRunConfiguration("temp",
                    JetRunConfigurationType.getInstance().getConfigurationFactories()[0]);
            try {
                String className = KotlinStudyUtils.getClassName(filePath);
                ((JetRunConfiguration) temp.getConfiguration()).setRunClass(className);
                RunProfileState state = temp.getConfiguration().getState(DefaultRunExecutor.getRunExecutorInstance(),
                        ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), temp).build());
                JavaCommandLineState javaCmdLine = (JavaCommandLineState) state;
                if (javaCmdLine == null) {
                    return;
                }
                JavaParameters javaParameters = javaCmdLine.getJavaParameters();
                GeneralCommandLine fromJavaParameters = CommandLineBuilder.createFromJavaParameters(javaParameters, project, false);
                cmd.setExePath(fromJavaParameters.getExePath());
                List<String> parameters = fromJavaParameters.getCommandLineList(fromJavaParameters.getExePath());
                cmd.addParameters(parameters.subList(1, parameters.size()));
            } catch (ExecutionException e) {
                LOG.error(e);
            }
        }
        else {
            showNoSdkNotification(project);
        }
    }

    public void showNoSdkNotification(@NotNull final Project project) {
        KotlinStudyUtils.showNoSdkNotification(project);
    }

    @Nullable
    @Override
    public StudyCheckAction getCheckAction() {
        return new KotlinStudyCheckAction();
    }

}