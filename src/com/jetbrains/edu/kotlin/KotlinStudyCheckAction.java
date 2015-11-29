package com.jetbrains.edu.kotlin;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.jetbrains.edu.EduUtils;
import com.jetbrains.edu.courseFormat.StudyStatus;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyRunAction;
import com.jetbrains.edu.learning.editor.StudyEditor;
import com.jetbrains.edu.learning.navigation.StudyNavigator;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import com.jetbrains.edu.stepic.EduStepicConnector;
import com.jetbrains.edu.stepic.StudySettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KotlinStudyCheckAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(StudyRunAction.class.getName());

    public static final String ACTION_ID = "KotlinStudyCheckAction";
    private static final String KOTLIN_EXTENSION = "kt";
    private static final String UTIL_FOLDER = "util";
    private static final String TEST_HELPER = "TestHelper.java";
    private ProcessHandler myHandler;
    private List<ProcessListener> myProcessListeners = new LinkedList<ProcessListener>();
    private boolean checkInProgress;

    public KotlinStudyCheckAction() {
        super("Run File With Tests", "Run your code with tests", AllIcons.General.Run);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = e.getProject();
        compileTaskFiles(project, dataContext);
        check(project);
    }

    public void check(@NotNull final Project project) {
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
//                        final IdeFrame frame = ((WindowManagerEx)WindowManager.getInstance()).findFrameFor(project);
//                        Compiling is background
//                        final StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();
//                        if (statusBar != null) {
//                            final List<Pair<TaskInfo, ProgressIndicator>> processes = statusBar.getBackgroundProcesses();
//                            if (!processes.isEmpty()) return;
//                        }

                        final Task task = studyState.getTask();
                        final VirtualFile taskDir = studyState.getTaskDir();
                        flushWindows(task, taskDir);
                        final StudyRunAction runAction = (StudyRunAction)ActionManager.getInstance().getAction(StudyRunAction.ACTION_ID);
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
                        Process testProcess = null;
                        try {
                            final VirtualFile executablePath = getTaskVirtualFile(studyState, task, taskDir);
                            if (executablePath != null) {
                                testProcess = testRunner.createCheckProcess(project, executablePath.getPath());
                            }
                        }
                        catch (ExecutionException e) {
                            LOG.error(e);
                        }
                        if (testProcess == null) {
                            return;
                        }
                        checkInProgress = true;
                        ProgressManager.getInstance().run(getCheckTask(studyState, testRunner, testProcess, project, selectedEditor));
                    }
                });
            }

            @Nullable
            private VirtualFile getTaskVirtualFile(@NotNull final StudyState studyState,
                                                   @NotNull final Task task,
                                                   @NotNull final VirtualFile taskDir) {
                VirtualFile taskVirtualFile = studyState.getVirtualFile();
                for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
                    String name = entry.getKey();
                    TaskFile taskFile = entry.getValue();
                    VirtualFile virtualFile = taskDir.findChild(name);
                    if (virtualFile != null) {
                        if (!taskFile.getAnswerPlaceholders().isEmpty()) {
                            taskVirtualFile = virtualFile;
                        }
                    }
                }
//                VirtualFile
                return taskVirtualFile;
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
//                drawAllPlaceholders(project, task, taskDir);
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
                                                //runSmartTestProcess(taskDir, testRunner, name, taskFile, project);
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

    private static void showTestResultPopUp(final String text, Color color, @NotNull final Project project) {
        BalloonBuilder balloonBuilder =
                JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, null, color, null);
        final Balloon balloon = balloonBuilder.createBalloon();
        StudyUtils.showCheckPopUp(project, balloon);
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


//  ----------------------------------------------Compile Functions----------------------------------------------
    private void compileTaskFiles(Project project, DataContext dataContext) {
        final Module module = LangDataKeys.MODULE_CONTEXT.getData(dataContext);
        if (module != null) {
            CompilerManager.getInstance(project).compile(module, null);
        } else {
            VirtualFile[] files = getCompilableFiles(project, dataContext);
            if (files.length > 0) {
                CompilerManager.getInstance(project).compile(files, null);
            }
        }
    }

    private static VirtualFile[] getCompilableFiles(Project project, DataContext dataContext) {
        VirtualFile taskFile = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)[0];
        ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
        for (VirtualFile file: taskFile.getParent().getChildren()) {
            if (file.getExtension() != null && file.getExtension().equals(KOTLIN_EXTENSION))
                files.add(file);
        }
        for (VirtualFile file: project.getBaseDir().getChildren()) {
            if (file.getName().equals(UTIL_FOLDER)) {
                for (VirtualFile file_h: file.getChildren()) {
                    if (file_h.getName().equals(TEST_HELPER)) {
                        files.add(file_h);
                    }
                }
            }
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }
}
