package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.projectView.CCCourseNode;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class RootNode extends ProjectViewProjectNode {
  @NotNull protected final Project myProject;

  public RootNode(@NotNull Project project,
                  ViewSettings viewSettings) {
    super(project, viewSettings);
    myProject = project;
  }

  @NotNull
  @Override
  public Collection<AbstractTreeNode> getChildren() {
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return Collections.emptyList();
    }
    else {
      final ArrayList<AbstractTreeNode> nodes = new ArrayList<>();
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        final PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(myProject.getBaseDir());
        addCourseNode(course, nodes, psiDirectory);
      }
      else {
        List<VirtualFile> topLevelContentRoots = ProjectViewDirectoryHelper.getInstance(myProject).getTopLevelRoots();
        for (VirtualFile root : topLevelContentRoots) {
          final PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(root);
          addCourseNode(course, nodes, psiDirectory);
        }
      }
      return nodes;
    }
  }

  private void addCourseNode(Course course, ArrayList<AbstractTreeNode> nodes, PsiDirectory psiDirectory) {
    if (CCUtils.isCourseCreator(myProject)) {
      nodes.add(new CCCourseNode(myProject, psiDirectory, getSettings(), course));
    }
    else {
      nodes.add(new CourseNode(myProject, psiDirectory, getSettings(), course));
    }
  }
}
