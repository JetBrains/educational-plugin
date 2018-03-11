package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.projectView.CCCourseNode;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;


public class RootNode extends ProjectViewProjectNode {
  @NotNull protected final Project myProject;
  protected final Course myCourse;

  public RootNode(@NotNull Project project,
                  ViewSettings viewSettings,
                  @Nullable Course course) {
    super(project, viewSettings);
    myProject = project;
    myCourse = course;
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode> getChildren() {
    if (myCourse == null) {
      return Collections.emptyList();
    }
    else {
      final PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(myProject.getBaseDir());
      if (CCUtils.isCourseCreator(myProject)) {
        return Collections.singleton(new CCCourseNode(myProject, psiDirectory, getSettings(), myCourse));
      }
      return Collections.singleton(new CourseNode(myProject, psiDirectory, getSettings(), myCourse));
    }
  }

}
