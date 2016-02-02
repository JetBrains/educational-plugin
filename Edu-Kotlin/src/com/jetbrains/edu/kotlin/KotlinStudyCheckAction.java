package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyLanguageManager;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.editor.StudyEditor;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClass;

import java.io.File;
import java.util.Collection;

public class KotlinStudyCheckAction extends StudyCheckAction {
    private static final Logger LOG = Logger.getInstance(KotlinStudyCheckAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile taskFileVF = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (taskFileVF == null || project == null) {
            return;
        }
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk == null) {
            KotlinStudyUtils.showNoSdkNotification(project);
            return;
        }
        final Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course == null) {
            return;
        }
        StudyLanguageManager languageManager = StudyUtils.getLanguageManager(course);
        if (languageManager == null) {
            return;
        }
        final VirtualFile testsFile = taskFileVF.getParent().findChild(languageManager.getTestFileName());
        if (testsFile == null) {
            return;
        }
        CompilerManager.getInstance(project).compile(getFilesToCompile(project, taskFileVF), new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                if (errors != 0) {
                    KotlinStudyUtils.showNotification(project, "Code has compilation errors");
                    return;
                }
                RunnerAndConfigurationSettings javaTemplateConfiguration = produceRunConfiguration(project,
                        "javaTemplateConfiguration", ApplicationConfigurationType.getInstance());

                setProcessParameters(project,
                        ((ApplicationConfiguration) javaTemplateConfiguration.getConfiguration()), taskFileVF, testsFile);

                RunProfileState state = getState(javaTemplateConfiguration);

                if (state == null) {
                    //exception is logged inside method
                    return;
                }

                final JavaCommandLineState javaCmdLine = (JavaCommandLineState) state;
                FileDocumentManager.getInstance().saveAllDocuments();
                DumbService.getInstance(project).runWhenSmart(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JavaParameters javaParameters;
                            javaParameters = javaCmdLine.getJavaParameters();
                            GeneralCommandLine fromJavaParameters = CommandLineBuilder.createFromJavaParameters(javaParameters, project, false);
                            Process process = fromJavaParameters.createProcess();
                            check(project, process);
                        } catch (ExecutionException e) {
                            LOG.error(e);
                        }
                    }
                });

            }
        });
    }

    private void check(@NotNull final Project project, final Process testProcess) {
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
                            LOG.error("StudyCheckAction was invoked outside of study editor");
                            return;
                        }
                        final Task task = studyState.getTask();
                        final VirtualFile taskDir = studyState.getTaskDir();
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                IdeFocusManager.getInstance(project).requestFocus(studyState.getEditor().getComponent(), true);
                            }
                        });

                        final StudyTestRunner testRunner = StudyUtils.getTestRunner(task, taskDir);
                        //TODO: uncomment this after API CHANGE
                        //checkInProgress = true;
                        ProgressManager.getInstance().run(getCheckTask(studyState, testRunner, testProcess, "", project, selectedEditor));
                    }
                });
            }
        });
    }

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

    //TODO: refactor
    private VirtualFile[] getFilesToCompile(Project project, VirtualFile taskFileVF) {
        return new VirtualFile[]{ taskFileVF.getParent(),
                VfsUtil.findFileByIoFile(new File(project.getBasePath() +
                        FileUtil.toSystemDependentName("/util/" + "EduTestRunner.java")), false)
        };
    }


    private void setProcessParameters(Project project, ApplicationConfiguration configuration,
                                      VirtualFile taskFileVF, @NotNull VirtualFile testsFile) {
        configuration.setMainClassName(EduIntellijUtils.TEST_RUNNER);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(testsFile);
        Collection<KtClass> ktClasses = PsiTreeUtil.findChildrenOfType(psiFile, KtClass.class);
        for (KtClass ktClass : ktClasses) {
            String name = ktClass.getName();
            configuration.setProgramParameters(KotlinStudyUtils.getTestClass(taskFileVF) + name);
        }
    }
}
