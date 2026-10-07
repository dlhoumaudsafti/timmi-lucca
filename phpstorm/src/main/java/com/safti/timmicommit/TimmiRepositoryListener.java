package com.safti.timmicommit;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import org.jetbrains.annotations.NotNull;

/** Notifié par le plugin Git à chaque changement d'état d'un dépôt (commit, checkout, pull…). */
public final class TimmiRepositoryListener implements GitRepositoryChangeListener {

  private final Project project;

  public TimmiRepositoryListener(Project project) {
    this.project = project;
  }

  @Override
  public void repositoryChanged(@NotNull GitRepository repository) {
    if (!project.isDisposed()) {
      project.getService(TimmiCommitWatcher.class).onRepositoryChanged(repository);
    }
  }
}
