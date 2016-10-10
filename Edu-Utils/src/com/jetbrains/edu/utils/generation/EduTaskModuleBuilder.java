package com.jetbrains.edu.utils.generation;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EduTaskModuleBuilder extends JavaModuleBuilder {
    private static final Logger LOG = Logger.getInstance(EduTaskModuleBuilder.class);
    private final Task myTask;
    private final Module myUtilModule;

    public EduTaskModuleBuilder(String moduleDir, @NotNull String name, @NotNull Task task, @NotNull Module utilModule) {
        myTask = task;
        myUtilModule = utilModule;
        String taskName = EduNames.TASK + task.getIndex();
        //module name like lessoni-taski
        String moduleName = name + "-" + taskName;
        setName(moduleName);
        setModuleFilePath(FileUtil.join(moduleDir, taskName, moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION));
    }


    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        Module module = super.createModule(moduleModel);
        if (!createTaskContent()) {
            LOG.info("Failed to copy task content");
            return module;
        }
        addJUnitLib(module);
        ModuleRootModificationUtil.addDependency(module, myUtilModule);
        return module;
    }

    private void addJUnitLib(Module module) {
        ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
        List<String> defaultRoots = descriptor.getLibraryClassesRoots();
        final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
        ModuleRootModificationUtil.addModuleLibrary(module, descriptor.getPresentableName(), urls, Collections.emptyList());
    }

    private boolean createTaskContent() throws IOException {
        Course course = myTask.getLesson().getCourse();
        String directory = getModuleFileDirectory();
        if (directory == null) {
            return false;
        }
        VirtualFile moduleDir = VfsUtil.findFileByIoFile(new File(directory), true);
        if (moduleDir == null) {
            return false;
        }
        VirtualFile src = moduleDir.findChild(EduNames.SRC);
        if (src == null) {
            return false;
        }
        String courseResourcesDirectory = course.getCourseDirectory();
        String taskResourcesPath = FileUtil.join(courseResourcesDirectory, EduNames.LESSON + myTask.getLesson().getIndex(),
                EduNames.TASK + myTask.getIndex());
        FileUtil.copyDirContent(new File(taskResourcesPath), new File(src.getPath()));
        return true;
    }
}
