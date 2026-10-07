package com.safti.timmicommit;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** « Timmi : copier le ticket du dernier commit et ouvrir la saisie » (menu Tools, Ctrl+Shift+A). */
public final class OpenTimmiAction extends AnAction {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    ApplicationManager.getApplication().executeOnPooledThread(() -> openForLastCommit(project, file));
  }

  private static void openForLastCommit(@Nullable Project project, @Nullable VirtualFile file) {
    GitRepository repo = project == null ? null : pickRepository(project, file);
    String revision = repo == null ? null : repo.getCurrentRevision();
    if (revision == null) {
      TimmiCommit.handleCommit(project, "", null, true);
      return;
    }
    String message = "";
    try {
      message = GitCommits.getCommit(project, repo.getRoot(), revision).message();
    } catch (Exception ex) {
      TimmiCommit.LOG.info("Erreur lors de la lecture du commit " + revision + " : " + ex.getMessage());
    }
    TimmiCommit.handleCommit(project, message, repo.getCurrentBranchName(), true);
  }

  private static @Nullable GitRepository pickRepository(Project project, @Nullable VirtualFile file) {
    GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
    List<GitRepository> repositories = manager.getRepositories();
    if (repositories.size() <= 1) {
      return repositories.isEmpty() ? null : repositories.get(0);
    }
    if (file != null) {
      GitRepository repo = manager.getRepositoryForFileQuick(file);
      if (repo != null) {
        return repo;
      }
    }
    return repositories.get(0);
  }
}
