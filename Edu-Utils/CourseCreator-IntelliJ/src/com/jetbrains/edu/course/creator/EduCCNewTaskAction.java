package com.jetbrains.edu.course.creator;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.actions.CCCreateTask;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.utils.EduIntelliJNames;
import com.jetbrains.edu.utils.generation.EduTaskModuleBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduCCNewTaskAction extends CCCreateTask {
    @Nullable
    @Override
    protected PsiDirectory createItemDir(@NotNull Project project, @NotNull StudyItem item, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory, @NotNull Course course) {
        String lessonDirName = parentDirectory.getName();
        NewModuleAction newModuleAction = new NewModuleAction();
        Module lessonModule = ModuleManager.getInstance(project).findModuleByName(lessonDirName);
        Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduIntelliJNames.UTIL);
        if (lessonModule == null || utilModule == null) {
            return null;
        }
        Task task = (Task) item;
        newModuleAction.createModuleFromWizard(project, null, new AbstractProjectWizard("", project, "") {
            @Override
            public StepSequence getSequence() {
                return null;
            }

            @Override
            public ProjectBuilder getProjectBuilder() {
                return new EduTaskModuleBuilder(parentDirectory.getVirtualFile().getPath(), lessonDirName, task, utilModule);
            }
        });
        return null;
    }
}
