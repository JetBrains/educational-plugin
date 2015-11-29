package com.jetbrains.edu.kotlin;

import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.ExceptionFilter;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.run.StudyExecutor;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


public class KotlinStudyExecutor implements StudyExecutor {
    private static final String JAVA_BIN = "/bin/java.exe";

    public Sdk findSdk(@NotNull final Project project) {
        return KotlinStudyUtils.findSdk(project);
    }

    @Override
    public StudyTestRunner getTestRunner(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
        return new KotlinStudyTestRunner(task, taskDir);
    }

    @Override
    public RunContentExecutor getExecutor(@NotNull final Project project, @NotNull final ProcessHandler handler) {
//      TODO: find TracebackFilter
//        return new RunContentExecutor(project, handler).withFilter(new PythonTracebackFilter(project));
        return new RunContentExecutor(project, handler).withFilter(new ExceptionFilter(GlobalSearchScope.allScope(project)));
    }

    @Override
    public void setCommandLineParameters(@NotNull final GeneralCommandLine cmd,
                                         @NotNull final Project project,
                                         @NotNull final String filePath,
                                         @NotNull final String sdkPath,
                                         @NotNull final Task currentTask) {
        String classPath = KotlinStudyUtils.classFromSource(project, filePath);
        cmd.setExePath(sdkPath + FileUtil.toSystemDependentName(JAVA_BIN));
        cmd.withParameters("-classpath", KotlinStudyUtils.filePath(classPath), classPath);
        /*final List<UserTest> userTests = StudyTaskManager.getInstance(project).getUserTests(currentTask);
        if (!userTests.isEmpty()) {
            StudyLanguageManager manager = StudyUtils.getLanguageManager(currentTask.getLesson().getCourse());
            if (manager != null) {
                cmd.addParameter(new File(project.getBaseDir().getPath(), manager.getUserTester()).getPath());
                cmd.addParameter(sdkPath);
                cmd.addParameter(filePath);
            }
        }
        else {
            cmd.addParameter(filePath);
        }*/
    }

    public void showNoSdkNotification(@NotNull final Project project) {
        final String text = "<html>No Java SDK configured for the project<br><a href=\"\">Configure SDK</a></html>";
        final BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().
                createHtmlTextBalloonBuilder(text, null,
                        MessageType.WARNING.getPopupBackground(),
                        new HyperlinkListener() {
                            @Override
                            public void hyperlinkUpdate(HyperlinkEvent event) {
                                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                    ApplicationManager.getApplication()
                                            .invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Project SDK");
                                                }
                                            });
                                }
                            }
                        });
        balloonBuilder.setHideOnLinkClick(true);
        final Balloon balloon = balloonBuilder.createBalloon();
        StudyUtils.showCheckPopUp(project, balloon);
    }

}