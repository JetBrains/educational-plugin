package com.jetbrains.edu.kotlin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.io.File;

public class KotlinStudyUtils {

    public static String getPackageNamePrefix(VirtualFile file) {
        VirtualFile cur = file.getParent();
        return cur.getName() + ".";
    }

    public static String getPackageNamePrefix(String classPath) {
        final VirtualFile taskFileVF = VfsUtil.findFileByIoFile(new File(classPath), false);
        return getPackageNamePrefix(taskFileVF);
    }

    public static String getClassName(String sourcePath) {
        String className = FileUtil.toSystemIndependentName(FileUtil.getNameWithoutExtension(sourcePath));
        if (FileUtilRt.getExtension(sourcePath).equals("kt"))
            className += "Kt";
        String packageName = getPackageNamePrefix(sourcePath);
        className = className.substring(className.lastIndexOf('/') + 1);
        className = className.substring(0, 1).toUpperCase() + className.substring(1);
        return packageName + className;
    }

    public static void showNotification(@NotNull final Project project, final String text) {
        final BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().
                createHtmlTextBalloonBuilder(text, null,
                        MessageType.WARNING.getPopupBackground(),
                        null);
        balloonBuilder.setHideOnLinkClick(true);
        final Balloon balloon = balloonBuilder.createBalloon();
        StudyUtils.showCheckPopUp(project, balloon);
    }

    public static void showNoSdkNotification(@NotNull final Project project) {
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
//                                                    TODO: find project structure dialog
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

    public static String getTestClass(VirtualFile taskFile) {
        return getPackageNamePrefix(taskFile) + "tests.";
    }
}
