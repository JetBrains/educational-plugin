package com.jetbrains.edu.kotlin;

import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static Sdk findSdk(@Nullable Module module) {
        if (module == null) return null;
        final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk != null && sdk.getSdkType() instanceof JavaSdk) return sdk;
        return null;
    }

    public static Sdk findSdk(@NotNull final Project project) {
        return findSdk(ModuleManager.getInstance(project).getModules()[0]);
    }

    public static String filePath(String file) {
        String indFile = FileUtil.toSystemIndependentName(file);
        return FileUtil.toSystemDependentName(indFile.substring(0, indFile.lastIndexOf('/')));
    }

    public static String classFromSource(@NotNull final Project project, String source) {
        String extension = FileUtil.getExtension(source);
        String classPath = FileUtil.toSystemIndependentName(project.getBasePath()) + "/out/production/" + project.getName() + "/";
        String className = FileUtil.toSystemIndependentName(FileUtil.getNameWithoutExtension(source));
        if (extension.equals("kt"))
            className += "Kt";
        className = className.substring(className.lastIndexOf('/') + 1);
        String res = FileUtil.toSystemDependentName(classPath + className + ".class");
        return res;
    }

    public static File classFromSource(@NotNull final Project project, File source) {
        File res = new File(classFromSource(project, FileUtil.toSystemIndependentName(source.getPath())));
        return res;
    }
}
