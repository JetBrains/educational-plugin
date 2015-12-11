package com.jetbrains.edu.kotlin;

import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.io.File;

public class KotlinStudyUtils {
    private static  final Logger LOG = Logger.getInstance(KotlinStudyUtils.class);

    public static void commitAndSaveModel(final ModifiableRootModel model) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                model.commit();
                model.getProject().save();
            }
        });
    }

    @Nullable
    public static ModifiableRootModel getModel(@NotNull VirtualFile dir, @NotNull Project project) {
        final Module module = ModuleUtilCore.findModuleForFile(dir, project);
        if (module == null) {
            LOG.info("Module for " + dir.getPath() + " was not found");
            return null;
        }
        return ModuleRootManager.getInstance(module).getModifiableModel();
    }

    public static void markDirAsSourceRoot(@NotNull final VirtualFile dir, @NotNull final Project project) {
        final ModifiableRootModel model = getModel(dir, project);
        if (model == null) {
            return;
        }
        final ContentEntry entry = MarkRootActionBase.findContentEntry(model, dir);
        if (entry == null) {
            LOG.info("Content entry for " + dir.getPath() + " was not found");
            return;
        }
        entry.addSourceFolder(dir, false);
        commitAndSaveModel(model);
    }

    public static String getPackageName(VirtualFile file, final Project project) {
        String packageName = "";
        VirtualFile cur = file.getParent();
        while (!cur.getName().equals(project.getName())) {
            packageName = cur.getName() + "." + packageName;
            cur = cur.getParent();
        }
        return packageName;
    }

    public static String getPackageName(String classPath, final Project project) {
        final VirtualFile taskFileVF = VfsUtil.findFileByIoFile(new File(classPath), false);
        return getPackageName(taskFileVF, project);
    }

    public static String getClassName(String sourcePath, final Project project) {
        String className = FileUtil.toSystemIndependentName(FileUtil.getNameWithoutExtension(sourcePath));
        if (FileUtil.getExtension(sourcePath).equals("kt"))
            className += "Kt";
        String packageName = getPackageName(sourcePath, project);
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

    public static String getTestClass(VirtualFile taskFile, final Project project) {
        return getPackageName(taskFile, project) + "tests.TestsKt";
    }
}
