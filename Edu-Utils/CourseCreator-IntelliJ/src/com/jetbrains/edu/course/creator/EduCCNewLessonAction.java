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
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.utils.EduIntelliJNames;
import com.jetbrains.edu.utils.generation.EduLessonModuleBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduCCNewLessonAction extends CCCreateLesson {
    @Nullable
    @Override
    protected PsiDirectory createItemDir(@NotNull Project project, @NotNull StudyItem item, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory, @NotNull Course course) {
        NewModuleAction newModuleAction = new NewModuleAction();
        String courseDirPath = parentDirectory.getVirtualFile().getPath();
        Lesson lesson = (Lesson) item;
        Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduIntelliJNames.UTIL);
        if (utilModule == null) {
            return null;
        }
        newModuleAction.createModuleFromWizard(project, null, new AbstractProjectWizard("", project, "") {
            @Override
            public StepSequence getSequence() {
                return null;
            }

            @Override
            public ProjectBuilder getProjectBuilder() {
                return new EduLessonModuleBuilder(courseDirPath, lesson, utilModule);
            }
        });
        return null;
    }
}
