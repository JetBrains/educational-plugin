package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

public class ProjectNode extends EduNode {
  public ProjectNode(@NotNull Project project,
                     PsiDirectory value,
                     ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  public boolean canNavigate() {
    return true;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    super.updateImpl(data);
    data.setIcon(EducationalCoreIcons.Playground);
  }
}
