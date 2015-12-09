package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.FileContentUtil;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.actions.StudyRunAction;
import com.jetbrains.edu.learning.editor.StudyEditor;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.run.JetRunConfiguration;
import org.jetbrains.kotlin.idea.run.JetRunConfigurationType;

import java.io.File;
import java.util.Arrays;


public class KotlinStudyCheckAction extends StudyCheckAction {

    private static final Logger LOG = Logger.getInstance(StudyRunAction.class.getName());
    public static final String ACTION_ID = "KotlinStudyCheckAction";

    boolean checkInProgress = false;

    private void setProcessParameters(Project project, RunnerAndConfigurationSettings settings, VirtualFile taskFileVF) {
        final StudyEditor selectedEditor = StudyUtils.getSelectedStudyEditor(project);
        if (selectedEditor == null) return;
        final StudyState studyState = new StudyState(selectedEditor);
        if (!studyState.isValid()) {
            LOG.error("StudyCheckAction was invoked outside study editor");
            return;
        }
        final Task task = studyState.getTask();
        Course course = task.getLesson().getCourse();
        String className = KotlinStudyUtils.getTestClass(taskFileVF, project);
        ((JetRunConfiguration) settings.getConfiguration()).setRunClass(className);
        File resourceFile = new File(course.getCourseDirectory());
        ((JetRunConfiguration) settings.getConfiguration()).setProgramParameters(
                "\"" + FileUtil.toSystemDependentName(resourceFile.getPath()) + "\" " +
                        FileUtil.toSystemDependentName(taskFileVF.getPath()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final VirtualFile taskFileVF = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (taskFileVF == null || project == null) {
            return;
        }
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk == null) {
            KotlinStudyUtils.showNoSdkNotification(project);
            return;
        }

        CompilerManager.getInstance(project).make(ModuleManager.getInstance(project).getModules()[0], new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                if (errors != 0) {
                    LOG.error("Compilation wasn't finished because of errors in the code");
                    KotlinStudyUtils.showNotification(project, "Compilation wasn't finished because of your code contains errors. Please, fix it.");
                    return;
                }
                RunnerAndConfigurationSettings temp = RunManager.getInstance(project).createRunConfiguration("temp",
                        JetRunConfigurationType.getInstance().getConfigurationFactories()[0]);
                setProcessParameters(project, temp, taskFileVF);
                RunProfileState state = null;
                try {
                    state = temp.getConfiguration().getState(DefaultRunExecutor.getRunExecutorInstance(), ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), temp).build());

                    final JavaCommandLineState javaCmdLine = (JavaCommandLineState) state;
                    if (javaCmdLine == null) {
                        return;
                    }
                    FileDocumentManager.getInstance().saveAllDocuments();
                    PsiDocumentManager.getInstance(project).reparseFiles(Arrays.asList(taskFileVF.getParent().getChildren()), true);
                    FileContentUtil.reparseFiles(project, Arrays.asList(taskFileVF.getParent().getChildren()), true);
                    DumbService.getInstance(project).runWhenSmart(new Runnable() {
                        @Override
                        public void run() {
                            JavaParameters javaParameters = null;
                            try {
                                javaParameters = javaCmdLine.getJavaParameters();
                                GeneralCommandLine fromJavaParameters = CommandLineBuilder.createFromJavaParameters(javaParameters, project, false);
                                Process process = fromJavaParameters.createProcess();
                                check(project, process);

                            } catch (ExecutionException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                } catch (ExecutionException e1) {
                    LOG.error(e1);
                }
            }
        });
    }

    public void check(@NotNull final Project project, @NotNull final Process testProcess) {
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).showDumbModeNotification("Check Action is not available while indexing is in progress");
            return;
        }
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
                    @Override
                    public void run() {
                        final StudyEditor selectedEditor = StudyUtils.getSelectedStudyEditor(project);
                        if (selectedEditor == null) return;
                        final StudyState studyState = new StudyState(selectedEditor);
                        if (!studyState.isValid()) {
                            LOG.error("StudyCheckAction was invoked outside study editor");
                            return;
                        }
                        final Task task = studyState.getTask();
                        final VirtualFile taskDir = studyState.getTaskDir();
                        flushWindows(task, taskDir);
                        final StudyRunAction runAction = (StudyRunAction) ActionManager.getInstance().getAction(StudyRunAction.ACTION_ID);
                        if (runAction == null) {
                            return;
                        }
                        runAction.run(project);
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                IdeFocusManager.getInstance(project).requestFocus(studyState.getEditor().getComponent(), true);
                            }
                        });

                        final StudyTestRunner testRunner = StudyUtils.getTestRunner(task, taskDir);
                        checkInProgress = true;
                        ProgressManager.getInstance().run(getCheckTask(studyState, testRunner, testProcess, "", project, selectedEditor));
                    }
                });
            }
        });
    }
}
