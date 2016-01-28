package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.FileContentUtil;
import com.jetbrains.edu.core.EduIntellijUtils;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyLanguageManager;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.actions.StudyRunAction;
import com.jetbrains.edu.learning.editor.StudyEditor;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClass;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class KotlinStudyCheckAction extends StudyCheckAction {

    private static final Logger LOG = Logger.getInstance(StudyRunAction.class.getName());
    boolean checkInProgress = false;

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
                    LOG.error("Compilation wasn't finished because of errors in the code");
                    KotlinStudyUtils.showNotification(project, "Compilation wasn't finished because of your code contains errors. Please, fix it.");
                    return;
                }
                RunnerAndConfigurationSettings javaTemplateConfiguration = RunManager.getInstance(project).createRunConfiguration("javaTemplateConfiguration",
                        ApplicationConfigurationType.getInstance().getConfigurationFactories()[0]);

                setProcessParameters(project, javaTemplateConfiguration, taskFileVF, testsFile);
                RunProfileState state;
                try {
                    state = javaTemplateConfiguration.getConfiguration().
                            getState(DefaultRunExecutor.getRunExecutorInstance(),
                                    ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(),
                                            javaTemplateConfiguration).build());

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
                            JavaParameters javaParameters;
                            try {
                                javaParameters = javaCmdLine.getJavaParameters();
                                GeneralCommandLine fromJavaParameters = CommandLineBuilder.createFromJavaParameters(javaParameters, project, false);
                                Process process = fromJavaParameters.createProcess();
                                check(project, process);

                            } catch (ExecutionException e1) {
                                LOG.error(e1);
                            }
                        }
                    });
                } catch (ExecutionException e1) {
                    LOG.error(e1);
                }
            }
        });
    }

    private VirtualFile[] getFilesToCompile(Project project, VirtualFile taskFileVF) {
        return new VirtualFile[]{
                taskFileVF.getParent(),
                VfsUtil.findFileByIoFile(new File(project.getBasePath() + FileUtil.toSystemDependentName("/util/" + "EduTestRunner.java")), false)
        };
    }

    private void setProcessParameters(Project project, RunnerAndConfigurationSettings settings, VirtualFile taskFileVF, @NotNull VirtualFile testsFile) {
        ApplicationConfiguration configuration = (ApplicationConfiguration) settings.getConfiguration();
        configuration.setMainClassName(EduIntellijUtils.TEST_RUNNER);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(testsFile);
        Collection<KtClass> ktClasses = PsiTreeUtil.findChildrenOfType(psiFile, KtClass.class);
        for (KtClass ktClass : ktClasses) {
            String name = ktClass.getName();
            configuration.setProgramParameters(KotlinStudyUtils.getTestClass(taskFileVF, project) + name);
        }
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
