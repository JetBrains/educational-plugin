package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;


public class EduTaskModuleBuilder extends EduBaseIntellijModuleBuilder {
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
    Course course = myTask.getLesson().getCourse();
    String directory = getModuleFileDirectory();
    if (directory == null) {
      return module;
    }
    VirtualFile moduleDir = VfsUtil.findFileByIoFile(new File(directory), true);
    if (moduleDir == null) {
      return module;
    }
    VirtualFile src = moduleDir.findChild(EduNames.SRC);
    if (src == null) {
      return module;
    }
    createTask(module.getProject(), course, src);
    ModuleRootModificationUtil.addDependency(module, myUtilModule);
    return module;
  }

  protected void createTask(Project project, Course course, VirtualFile src) throws IOException {
    GeneratorUtils.createTaskContent(myTask, src);
  }

  @Nullable
  @Override
  protected Course getCourse() {
    return myTask.getLesson().getCourse();
  }
}
