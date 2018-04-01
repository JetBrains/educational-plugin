package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.projectView.LessonNode;
import com.jetbrains.edu.learning.projectView.SectionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CCSectionNode extends SectionNode {
  public CCSectionNode(@NotNull Project project, @NotNull ViewSettings viewSettings, @NotNull Section section, @Nullable PsiDirectory psiDirectory) {
    super(project, viewSettings, section, psiDirectory);
  }

  @Override
  @NotNull
  protected LessonNode createLessonNode(@NotNull PsiDirectory directory, @NotNull Lesson lesson) {
    return new CCLessonNode(myProject, directory, getSettings(), lesson);
  }
}
