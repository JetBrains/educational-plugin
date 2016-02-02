package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.PsiEditorUtil;
import com.intellij.psi.util.PsiUtil;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.run.JetRunConfiguration;
import org.jetbrains.kotlin.idea.run.JetRunConfigurationType;

import java.io.File;


public class KotlinStudyTestRunner extends StudyTestRunner {
    private static final Logger LOG = Logger.getInstance(KotlinStudyTestRunner.class);

    public KotlinStudyTestRunner(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
        super(task, taskDir);
    }

    public Process createCheckProcess(@NotNull final Project project, @NotNull final String executablePath) throws ExecutionException {
        final VirtualFile taskFileVF = VfsUtil.findFileByIoFile(new File(executablePath), false);

        if (taskFileVF == null) {
            return null;
        }

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                executeFile(taskFileVF, project);
            }
        });

        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk != null) {
            RunnerAndConfigurationSettings temp = RunManager.getInstance(project).createRunConfiguration("temp", JetRunConfigurationType.getInstance().getConfigurationFactories()[0]);
            try {
                String className = KotlinStudyUtils.getTestClass(taskFileVF);
                ((JetRunConfiguration) temp.getConfiguration()).setRunClass(className);
                RunProfileState state = temp.getConfiguration().getState(DefaultRunExecutor.getRunExecutorInstance(), ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), temp).build());
                JavaCommandLineState javaCmdLine = (JavaCommandLineState) state;
                if (javaCmdLine == null) {
                    return null;
                }
                JavaParameters javaParameters = javaCmdLine.getJavaParameters();
                GeneralCommandLine fromJavaParameters = CommandLineBuilder.createFromJavaParameters(javaParameters, project, false);
                return fromJavaParameters.createProcess();
            } catch (ExecutionException e) {
                LOG.error(e);
            }
        }
        return null;
    }

    private void executeFile(@NotNull final VirtualFile taskFileVF, Project project) {
        Editor selectedEditor = PsiEditorUtil.Service.getInstance().findEditorByPsiElement(PsiUtil.getPsiFile(project, taskFileVF));
        if (selectedEditor != null) {
            ConfigurationContext context = ConfigurationContext.getFromContext(DataManager.getInstance().
                    getDataContext(selectedEditor.getComponent()));
            RunnerAndConfigurationSettings configuration = context.getConfiguration();
            if (configuration != null) {
                ExecutionUtil.runConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance());
            }
        }
    }
}