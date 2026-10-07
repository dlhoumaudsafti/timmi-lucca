package com.safti.timmicommit;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recherche du ticket, copie dans le presse-papier et ouverture de Timmi. */
public final class TimmiCommit {

  static final Logger LOG = Logger.getInstance("Timmi Commit");

  private static final Pattern ANY_TICKET = Pattern.compile("\\b[A-Z][A-Z0-9_]+-\\d+\\b", Pattern.CASE_INSENSITIVE);

  // Commandes / applications candidates par navigateur et par plateforme.
  private static final Map<String, List<String>> BROWSERS_LINUX = Map.of(
      "firefox", List.of("firefox", "firefox-esr"),
      "chrome", List.of("google-chrome", "google-chrome-stable"),
      "chromium", List.of("chromium", "chromium-browser"),
      "edge", List.of("microsoft-edge", "microsoft-edge-stable"),
      "brave", List.of("brave-browser", "brave"));
  private static final Map<String, List<String>> BROWSERS_MAC = Map.of(
      "firefox", List.of("Firefox"),
      "chrome", List.of("Google Chrome"),
      "chromium", List.of("Chromium"),
      "edge", List.of("Microsoft Edge"),
      "brave", List.of("Brave Browser"));
  private static final Map<String, List<String>> BROWSERS_WINDOWS = Map.of(
      "firefox", List.of("firefox"),
      "chrome", List.of("chrome"),
      "chromium", List.of("chromium"),
      "edge", List.of("msedge"),
      "brave", List.of("brave"));

  private TimmiCommit() {}

  /**
   * À appeler hors EDT (le lancement du navigateur peut attendre la fin d'un processus).
   *
   * @param message message du commit
   * @param branch  nom de la branche courante
   * @param manual  déclenché par l'action (ignore le réglage « enabled »)
   */
  static void handleCommit(@Nullable Project project, String message, @Nullable String branch, boolean manual) {
    TimmiSettings.State config = TimmiSettings.getInstance().getState();
    if (!manual && !config.enabled) {
      return;
    }

    Pattern regex = buildTicketRegex(config.team, config.customTeam);
    String ticket = findTicket(message, regex);
    if (ticket == null && branch != null && config.searchBranchName) {
      ticket = findTicket(branch, regex);
    }

    if (ticket != null) {
      String copied = ticket;
      ApplicationManager.getApplication().invokeLater(
          () -> CopyPasteManager.getInstance().setContents(new StringSelection(copied)));
      LOG.info("Ticket " + ticket + " copié dans le presse-papier.");
    } else {
      LOG.info("Aucun ticket trouvé dans « " + firstLine(message) + " ».");
      if (!manual && !config.openWithoutTicket) {
        return;
      }
    }

    String status = ticket != null
        ? "Ticket " + ticket + " copié dans le presse-papier."
        : "Aucun numéro de ticket trouvé dans le commit.";

    if (config.askBeforeOpening && !manual) {
      notification(status, NotificationType.INFORMATION)
          .addAction(NotificationAction.createSimpleExpiring("Ouvrir Timmi",
              () -> ApplicationManager.getApplication().executeOnPooledThread(() -> openBrowser(project))))
          .notify(project);
      return;
    }

    notification(status + " Ouverture de Timmi…", NotificationType.INFORMATION).notify(project);
    openBrowser(project);
  }

  public static Pattern buildTicketRegex(String team, String customTeam) {
    if ("AUTRE".equals(team)) {
      team = customTeam == null ? "" : customTeam.trim();
    }
    if (team == null || team.isEmpty() || "TOUTES".equals(team)) {
      return ANY_TICKET;
    }
    return Pattern.compile("\\b" + Pattern.quote(team) + "-\\d+\\b", Pattern.CASE_INSENSITIVE);
  }

  public static @Nullable String findTicket(@Nullable String text, Pattern regex) {
    Matcher matcher = regex.matcher(text == null ? "" : text);
    return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : null;
  }

  private static String firstLine(@Nullable String text) {
    return text == null ? "" : text.split("\n", -1)[0];
  }

  private static Notification notification(String content, NotificationType type) {
    return NotificationGroupManager.getInstance().getNotificationGroup("Timmi Commit")
        .createNotification("Timmi Commit", content, type);
  }

  private static void openBrowser(@Nullable Project project) {
    TimmiSettings.State config = TimmiSettings.getInstance().getState();
    String url = config.url == null || config.url.isBlank() ? TimmiSettings.DEFAULT_URL : config.url.trim();
    String browser = config.browser == null ? "default" : config.browser;

    if (!"default".equals(browser)) {
      boolean opened = "custom".equals(browser)
          ? launchCustom(config.browserPath == null ? "" : config.browserPath.trim(), url)
          : launchKnown(browser, url);
      if (opened) {
        return;
      }
      notification("Impossible de lancer le navigateur « " + browser
          + " » : ouverture avec le navigateur par défaut.", NotificationType.WARNING).notify(project);
    }

    BrowserUtil.browse(url);
  }

  private static boolean launchKnown(String browser, String url) {
    Map<String, List<String>> browsers = SystemInfo.isMac ? BROWSERS_MAC
        : SystemInfo.isWindows ? BROWSERS_WINDOWS
        : BROWSERS_LINUX;
    for (String candidate : browsers.getOrDefault(browser, List.of())) {
      boolean ok;
      if (SystemInfo.isMac) {
        ok = runToCompletion("open", "-a", candidate, url);
      } else if (SystemInfo.isWindows) {
        ok = runToCompletion("cmd", "/c", "start", "\"\"", candidate, url);
      } else {
        ok = launchDetached(candidate, url);
      }
      if (ok) {
        LOG.info("Timmi ouvert avec " + candidate + ".");
        return true;
      }
    }
    return false;
  }

  private static boolean launchCustom(String path, String url) {
    if (path.isEmpty()) {
      LOG.info("Navigateur « custom » sélectionné mais aucun chemin configuré.");
      return false;
    }
    if (SystemInfo.isMac && path.endsWith(".app")) {
      return runToCompletion("open", "-a", path, url);
    }
    return launchDetached(path, url);
  }

  /** Lance un processus détaché ; renvoie true dès qu'il a démarré. */
  private static boolean launchDetached(String... command) {
    try {
      new ProcessBuilder(command)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
      return true;
    } catch (IOException e) {
      LOG.info("Échec du lancement de " + command[0] + " : " + e.getMessage());
      return false;
    }
  }

  /** Lance une commande courte (open / start) ; renvoie true si elle se termine avec le code 0. */
  private static boolean runToCompletion(String... command) {
    try {
      Process process = new ProcessBuilder(command)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
      return process.waitFor() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
