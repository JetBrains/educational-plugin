package com.jetbrains.edu.utils.generation;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.EduNames;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EduLessonModuleBuilder extends JavaModuleBuilder {
    private final Lesson myLesson;
    private final Module myUtilModule;

    public EduLessonModuleBuilder(@NotNull String moduleDir, @NotNull Lesson lesson, @NotNull Module utilModule) {
        myLesson = lesson;
        myUtilModule = utilModule;
        String lessonName = EduNames.LESSON + lesson.getIndex();
        setName(lessonName);
        setModuleFilePath(FileUtil.join(moduleDir, lessonName, lessonName + ModuleFileType.DOT_DEFAULT_EXTENSION));
    }

    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        Module baseModule = super.createModule(moduleModel);
        List<Task> taskList = myLesson.getTaskList();
        for (int i = 0; i < taskList.size(); i++) {
            int visibleTaskIndex = i + 1;
            Task task = taskList.get(i);
            task.setIndex(visibleTaskIndex);
            createTaskModule(moduleModel, task);
        }
        return baseModule;

    }

    private void createTaskModule(@NotNull ModifiableModuleModel moduleModel, @NotNull Task task) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        EduTaskModuleBuilder taskModuleBuilder = new EduTaskModuleBuilder(getModuleFileDirectory(), getName(), task, myUtilModule);
        taskModuleBuilder.createModule(moduleModel);
    }

    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        setSourcePaths(Collections.<Pair<String, String>>emptyList());
        super.setupRootModel(rootModel);
    }
}
