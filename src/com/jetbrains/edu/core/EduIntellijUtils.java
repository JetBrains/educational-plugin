package com.jetbrains.edu.core;

import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.io.ZipUtil;
import com.jetbrains.edu.kotlin.KotlinStudyModuleBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


public class EduIntellijUtils {
    private static final Logger LOG = Logger.getInstance(EduIntellijUtils.class);

    private EduIntellijUtils() {
    }

    public static File getBundledCourseRoot(final String courseName) {
        @NonNls String jarPath = PathUtil.getJarPathForClass(KotlinStudyModuleBuilder.class);
        if (jarPath.endsWith(".jar")) {
            final File jarFile = new File(jarPath);
            File pluginBaseDir = jarFile.getParentFile();
            File coursesDir = new File(pluginBaseDir, "courses");
            if (!coursesDir.exists()) {
                if (!coursesDir.mkdir()) {
                    LOG.info("Failed to create courses dir");
                } else {
                    try {
                        ZipUtil.extract(jarFile, pluginBaseDir, new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.equals(courseName);
                            }
                        });
                    } catch (IOException e) {
                        LOG.info("Failed to extract default course", e);
                    }
                }
            }
            return coursesDir;
        }
        return new File(jarPath, "courses");
    }

    private static void commitAndSaveModel(final ModifiableRootModel model) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                model.commit();
                model.getProject().save();
            }
        });
    }

    @Nullable
    private static ModifiableRootModel getModel(@NotNull VirtualFile dir, @NotNull Project project) {
        final Module module = ModuleUtilCore.findModuleForFile(dir, project);
        if (module == null) {
            LOG.info("Module for " + dir.getPath() + " was not found");
            return null;
        }
        return ModuleRootManager.getInstance(module).getModifiableModel();
    }

    static void markDirAsSourceRoot(@NotNull final VirtualFile dir, @NotNull final Project project) {
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
}
