package com.safti.timmicommit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Surveille le HEAD des dépôts du projet : un nouveau commit sur la même branche, dont le parent
 * est l'ancien HEAD, est considéré comme un commit local (UI de l'IDE ou terminal).
 */
@Service(Service.Level.PROJECT)
public final class TimmiCommitWatcher {

  // Un commit plus vieux que ça n'est pas considéré comme « nouveau » (pull, reset…).
  private static final long MAX_COMMIT_AGE_MS = 5 * 60 * 1000;

  // Partagé entre projets : un même commit n'ouvre Timmi qu'une fois.
  private static final Set<String> HANDLED_COMMITS = ConcurrentHashMap.newKeySet();

  private record Head(@Nullable String commit, @Nullable String branch) {}

  private final Project project;
  private final Map<String, Head> heads = new ConcurrentHashMap<>();

  public TimmiCommitWatcher(Project project) {
    this.project = project;
  }

  void onRepositoryChanged(GitRepository repo) {
    String root = repo.getRoot().getPath();
    Head head = new Head(repo.getCurrentRevision(), repo.getCurrentBranchName());
    Head previous;
    synchronized (heads) {
      previous = heads.get(root);
      if (previous == null) {
        TimmiCommit.LOG.info("Surveillance du dépôt " + root);
        heads.put(root, head);
        return;
      }
      if (head.commit() == null || head.commit().equals(previous.commit())) {
        return;
      }
      heads.put(root, head);
    }

    // previous.commit() est vide pour le premier commit d'un dépôt (branche sans commit).
    if (!Objects.equals(head.branch(), previous.branch()) || HANDLED_COMMITS.contains(head.commit())) {
      return;
    }

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        GitCommits.Commit commit = GitCommits.getCommit(project, repo.getRoot(), head.commit());
        if (!isNewLocalCommit(repo, commit, previous.commit()) || !HANDLED_COMMITS.add(commit.hash())) {
          return;
        }
        TimmiCommit.handleCommit(project, commit.message(), head.branch(), false);
      } catch (Exception e) {
        TimmiCommit.LOG.info("Erreur lors de l'analyse du commit " + head.commit() + " : " + e.getMessage());
      }
    });
  }

  private boolean isNewLocalCommit(GitRepository repo, GitCommits.Commit commit, @Nullable String previousCommit) {
    boolean isChild = previousCommit != null
        ? commit.parents().contains(previousCommit)
        : commit.parents().isEmpty();
    if (!isChild) {
      return false;
    }
    if (System.currentTimeMillis() - commit.commitDateMs() > MAX_COMMIT_AGE_MS) {
      return false;
    }
    // Pas d'e-mail configuré : on ne filtre pas sur l'auteur.
    String email = GitCommits.getUserEmail(project, repo.getRoot());
    return email == null || email.isBlank() || commit.authorEmail().isEmpty()
        || email.trim().equalsIgnoreCase(commit.authorEmail());
  }
}
