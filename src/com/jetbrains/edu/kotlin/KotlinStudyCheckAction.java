package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.FileContentUtil;
import com.jetbrains.edu.EduDocumentListener;
import com.jetbrains.edu.EduUtils;
import com.jetbrains.edu.courseFormat.*;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.actions.StudyRunAction;
import com.jetbrains.edu.learning.editor.StudyEditor;
import com.jetbrains.edu.learning.navigation.StudyNavigator;
import com.jetbrains.edu.learning.run.StudySmartChecker;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import com.jetbrains.edu.stepic.EduStepicConnector;
import com.jetbrains.edu.stepic.StudySettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.run.JetRunConfiguration;
import org.jetbrains.kotlin.idea.run.JetRunConfigurationType;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


public class KotlinStudyCheckAction extends StudyCheckAction {

    private static final Logger LOG = Logger.getInstance(StudyRunAction.class.getName());

    public static final String ACTION_ID = "KotlinStudyCheckAction";
    private static final String ANSWERS_POSTFIX = "_answers";

    boolean checkInProgress = false;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final VirtualFile taskFileVF = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (taskFileVF == null || project == null) {
            return;
        }

        CompilerManager.getInstance(project).make(ModuleManager.getInstance(project).getModules()[0], new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                RunnerAndConfigurationSettings temp = RunManager.getInstance(project).createRunConfiguration("temp",
                        JetRunConfigurationType.getInstance().getConfigurationFactories()[0]);
                // creating check process part
                final StudyEditor selectedEditor = StudyUtils.getSelectedStudyEditor(project);
                if (selectedEditor == null) return;
                final StudyState studyState = new StudyState(selectedEditor);
                if (!studyState.isValid()) {
                    LOG.error("StudyCheckAction was invoked outside study editor");
                    return;
                }
                final Task task = studyState.getTask();
                Course course = task.getLesson().getCourse();
                String className = "tests.TestsKt";
                ((JetRunConfiguration) temp.getConfiguration()).setRunClass(className);
                File resourceFile = new File(course.getCourseDirectory());
                ((JetRunConfiguration) temp.getConfiguration()).setProgramParameters(
                        "\"" + FileUtil.toSystemDependentName(resourceFile.getPath()) + "\" " +
                                FileUtil.toSystemDependentName(taskFileVF.getPath()));
                // end of the part
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
                        ProgressManager.getInstance().run(getCheckTask(studyState, testRunner, testProcess, project, selectedEditor));
                    }
                });
            }
        });
    }

    @NotNull
    private com.intellij.openapi.progress.Task.Backgroundable getCheckTask(final StudyState studyState,
                                                                           final StudyTestRunner testRunner,
                                                                           final Process testProcess,
                                                                           @NotNull final Project project,
                                                                           final StudyEditor selectedEditor) {
        final Task task = studyState.getTask();
        final VirtualFile taskDir = studyState.getTaskDir();

        final StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
        final StudyStatus statusBeforeCheck = taskManager.getStatus(task);
        return new com.intellij.openapi.progress.Task.Backgroundable(project, "Checking Task", true) {
            @Override
            public void onSuccess() {
                StudyUtils.updateToolWindows(project);
                drawAllPlaceholders(project, task, taskDir);
                ProjectView.getInstance(project).refresh();
                EduUtils.deleteWindowDescriptions(task, taskDir);
                checkInProgress = false;
            }

            @Override
            public void onCancel() {
                taskManager.setStatus(task, statusBeforeCheck);
                EduUtils.deleteWindowDescriptions(task, taskDir);
                checkInProgress = false;
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final Map<String, TaskFile> taskFiles = task.getTaskFiles();
                final CapturingProcessHandler handler = new CapturingProcessHandler(testProcess);
                final ProcessOutput output = handler.runProcessWithProgressIndicator(indicator);
                if (indicator.isCanceled()) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showTestResultPopUp("Tests check cancelled.", MessageType.WARNING.getPopupBackground(), project);
                        }
                    });
                    return;
                }
                final StudyTestRunner.TestsOutput testsOutput = testRunner.getTestsOutput(output);
                String stderr = output.getStderr();
                if (!stderr.isEmpty()) {
                    LOG.info("#educational " + stderr);
                }
                final StudySettings studySettings = StudySettings.getInstance();

                final String login = studySettings.getLogin();
                final String password = StringUtil.isEmptyOrSpaces(login) ? "" : studySettings.getPassword();
                if (testsOutput.isSuccess()) {
                    taskManager.setStatus(task, StudyStatus.Solved);
                    EduStepicConnector.postAttempt(task, true, login, password);
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showTestResultPopUp(testsOutput.getMessage(), MessageType.INFO.getPopupBackground(), project);
                        }
                    });
                }
                else {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (taskDir == null) return;
                            EduStepicConnector.postAttempt(task, false, login, password);
                            taskManager.setStatus(task, StudyStatus.Failed);
                            for (Map.Entry<String, TaskFile> entry : taskFiles.entrySet()) {
                                final String name = entry.getKey();
                                final TaskFile taskFile = entry.getValue();
                                if (taskFile.getAnswerPlaceholders().size() < 2) {
                                    taskManager.setStatus(taskFile, StudyStatus.Failed);
                                    continue;
                                }
                                CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                runSmartTestProcess(taskDir, testRunner, name, taskFile, project);
                                            }
                                        });
                                    }
                                });
                            }
                            showTestResultPopUp(testsOutput.getMessage(), MessageType.ERROR.getPopupBackground(), project);
                            navigateToFailedPlaceholder(studyState, task, taskDir, project);
                        }
                    });
                }
            }
        };
    }

    private static void navigateToFailedPlaceholder(@NotNull final StudyState studyState,
                                                    @NotNull final Task task,
                                                    @NotNull final VirtualFile taskDir,
                                                    @NotNull final Project project) {
        TaskFile selectedTaskFile = studyState.getTaskFile();
        Editor editor = studyState.getEditor();
        TaskFile taskFileToNavigate = selectedTaskFile;
        VirtualFile fileToNavigate = studyState.getVirtualFile();
        final StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
        if (!taskManager.hasFailedAnswerPlaceholders(selectedTaskFile)) {
            for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
                String name = entry.getKey();
                TaskFile taskFile = entry.getValue();
                if (taskManager.hasFailedAnswerPlaceholders(taskFile)) {
                    taskFileToNavigate = taskFile;
                    VirtualFile virtualFile = taskDir.findChild(name);
                    if (virtualFile == null) {
                        continue;
                    }
                    FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
                    if (fileEditor instanceof StudyEditor) {
                        StudyEditor studyEditor = (StudyEditor)fileEditor;
                        editor = studyEditor.getEditor();
                    }
                    fileToNavigate = virtualFile;
                    break;
                }
            }
        }
        if (fileToNavigate != null) {
            FileEditorManager.getInstance(project).openFile(fileToNavigate, true);
        }
        final Editor editorToNavigate = editor;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                IdeFocusManager.getInstance(project).requestFocus(editorToNavigate.getContentComponent(), true);
            }
        });

        StudyNavigator.navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate);
    }

    private void runSmartTestProcess(@NotNull final VirtualFile taskDir,
                                     @NotNull final StudyTestRunner testRunner,
                                     final String taskFileName,
                                     @NotNull final TaskFile taskFile,
                                     @NotNull final Project project) {
        final TaskFile answerTaskFile = new TaskFile();
        answerTaskFile.name = taskFileName;
        final VirtualFile virtualFile = taskDir.findChild(taskFileName);
        if (virtualFile == null) {
            return;
        }
        final VirtualFile answerFile = getCopyWithAnswers(taskDir, virtualFile, taskFile, answerTaskFile);
        for (final AnswerPlaceholder answerPlaceholder : answerTaskFile.getAnswerPlaceholders()) {
            final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) {
                continue;
            }
            if (!answerPlaceholder.isValid(document)) {
                continue;
            }
            StudySmartChecker.smartCheck(answerPlaceholder, project, answerFile, answerTaskFile, taskFile, testRunner,
                    virtualFile, document);
        }
        StudyUtils.deleteFile(answerFile);
    }

    private VirtualFile getCopyWithAnswers(@NotNull final VirtualFile taskDir,
                                           @NotNull final VirtualFile file,
                                           @NotNull final TaskFile source,
                                           @NotNull final TaskFile target) {
        VirtualFile copy = null;
        try {

            copy = file.copy(this, taskDir, file.getNameWithoutExtension() + ANSWERS_POSTFIX + "." + file.getExtension());
            final FileDocumentManager documentManager = FileDocumentManager.getInstance();
            final Document document = documentManager.getDocument(copy);
            if (document != null) {
                TaskFile.copy(source, target);
                EduDocumentListener listener = new EduDocumentListener(target);
                document.addDocumentListener(listener);
                for (AnswerPlaceholder answerPlaceholder : target.getAnswerPlaceholders()) {
                    if (!answerPlaceholder.isValid(document)) {
                        continue;
                    }
                    final int start = answerPlaceholder.getRealStartOffset(document);
                    final int end = start + answerPlaceholder.getLength();
                    final String text = answerPlaceholder.getPossibleAnswer();
                    document.replaceString(start, end, text);
                }
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        documentManager.saveDocument(document);
                    }
                });
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return copy;
    }

    private static void showTestResultPopUp(final String text, Color color, @NotNull final Project project) {
        BalloonBuilder balloonBuilder =
                JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, null, color, null);
        final Balloon balloon = balloonBuilder.createBalloon();
        StudyUtils.showCheckPopUp(project, balloon);
    }

    @Override
    public void update(AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        StudyUtils.updateAction(e);
        if (presentation.isEnabled()) {
            presentation.setEnabled(!checkInProgress);
        }
    }

    private static void flushWindows(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
        for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
            String name = entry.getKey();
            TaskFile taskFile = entry.getValue();
            VirtualFile virtualFile = taskDir.findChild(name);
            if (virtualFile == null) {
                continue;
            }
            EduUtils.flushWindows(taskFile, virtualFile, true);
        }
    }

    private static void drawAllPlaceholders(@NotNull final Project project, @NotNull final Task task, @NotNull final VirtualFile taskDir) {
        for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
            String name = entry.getKey();
            TaskFile taskFile = entry.getValue();
            VirtualFile virtualFile = taskDir.findChild(name);
            if (virtualFile == null) {
                continue;
            }
            FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
            if (fileEditor instanceof StudyEditor) {
                StudyEditor studyEditor = (StudyEditor)fileEditor;
                StudyUtils.drawAllWindows(studyEditor.getEditor(), taskFile);
            }
        }
    }

}
