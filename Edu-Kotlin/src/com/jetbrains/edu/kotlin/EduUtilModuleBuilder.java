package com.jetbrains.edu.kotlin;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.utils.EduIntellijUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EduUtilModuleBuilder extends JavaModuleBuilder {

    public EduUtilModuleBuilder(String moduleDir) {
        setName("util");
        setModuleFilePath(FileUtil.join(moduleDir, "util", "util" + ModuleFileType.DOT_DEFAULT_EXTENSION));
    }

    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        Module baseModule = super.createModule(moduleModel);
        String directory = getModuleFileDirectory();
        if (directory == null) {
            return baseModule;
        }
        VirtualFile moduleDir = VfsUtil.findFileByIoFile(new File(directory), true);
        if (moduleDir == null) {
            return baseModule;
        }
        VirtualFile src = moduleDir.findChild("src");
        if (src == null) {
            return baseModule;
        }
        Project project = baseModule.getProject();
        StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        EduIntellijUtils.addTemplate(project, src, "EduTestRunner.java");
                    }
                });
            }
        });
        ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
        List<String> defaultRoots = descriptor.getLibraryClassesRoots();
        final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
        ModuleRootModificationUtil.addModuleLibrary(baseModule, descriptor.getPresentableName(), urls, Collections.<String>emptyList());

        String courseDirectory = StudyTaskManager.getInstance(project).getCourse().getCourseDirectory();
        FileUtil.copyDirContent(new File(courseDirectory, "util"), new File(src.getPath()));
        return baseModule;
    }
}
