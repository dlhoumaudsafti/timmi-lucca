package com.safti.timmicommit;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Objects;

public final class TimmiConfigurable implements Configurable {

  /** Valeur stockée + libellé affiché dans la liste déroulante. */
  private record Choice(String value, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private static final Choice[] TEAMS = {
      new Choice("ALPHA", "ALPHA — tickets ALPHA-1234"),
      new Choice("BETA", "BETA — tickets BETA-1234"),
      new Choice("DELTA", "DELTA — tickets DELTA-1234"),
      new Choice("EPSILON", "EPSILON — tickets EPSILON-1234"),
      new Choice("GAMMA", "GAMMA — tickets GAMMA-1234"),
      new Choice("LAMBDA", "LAMBDA — tickets LAMBDA-1234"),
      new Choice("AUTRE", "AUTRE — utiliser le champ « Équipe personnalisée »"),
      new Choice("TOUTES", "TOUTES — n'importe quel ticket au format XXX-1234")
  };

  private static final Choice[] BROWSERS = {
      new Choice("default", "Navigateur par défaut du système"),
      new Choice("firefox", "Mozilla Firefox"),
      new Choice("chrome", "Google Chrome"),
      new Choice("chromium", "Chromium"),
      new Choice("edge", "Microsoft Edge"),
      new Choice("brave", "Brave"),
      new Choice("custom", "Personnalisé — exécutable défini dans « Chemin du navigateur »")
  };

  private JBCheckBox enabled;
  private ComboBox<Choice> team;
  private JBTextField customTeam;
  private ComboBox<Choice> browser;
  private JBTextField browserPath;
  private JBTextField url;
  private JBCheckBox searchBranchName;
  private JBCheckBox askBeforeOpening;
  private JBCheckBox openWithoutTicket;

  @Override
  public @Nls String getDisplayName() {
    return "Timmi Commit";
  }

  @Override
  public @Nullable JComponent createComponent() {
    enabled = new JBCheckBox("Activer l'ouverture de Timmi après chaque commit");
    team = new ComboBox<>(TEAMS);
    customTeam = new JBTextField();
    browser = new ComboBox<>(BROWSERS);
    browserPath = new JBTextField();
    url = new JBTextField();
    searchBranchName = new JBCheckBox(
        "Si aucun ticket n'est trouvé dans le message de commit, le chercher dans le nom de la branche");
    askBeforeOpening = new JBCheckBox(
        "Demander confirmation (notification) avant d'ouvrir le navigateur au lieu de l'ouvrir directement");
    openWithoutTicket = new JBCheckBox("Ouvrir Timmi même si aucun numéro de ticket n'est trouvé");

    team.addActionListener(e -> updateEnabledFields());
    browser.addActionListener(e -> updateEnabledFields());

    JPanel panel = FormBuilder.createFormBuilder()
        .addComponent(enabled)
        .addLabeledComponent("Équipe :", team)
        .addTooltip("Préfixe du ticket Jira à rechercher dans le message de commit.")
        .addLabeledComponent("Équipe personnalisée :", customTeam)
        .addTooltip("Préfixe de ticket libre (ex. OMEGA). Utilisé uniquement si Équipe vaut AUTRE.")
        .addLabeledComponent("Navigateur :", browser)
        .addLabeledComponent("Chemin du navigateur :", browserPath)
        .addTooltip("Chemin ou commande du navigateur (ex. /usr/bin/vivaldi). Utilisé uniquement si Navigateur vaut Personnalisé.")
        .addLabeledComponent("URL :", url)
        .addTooltip("URL de la page de saisie des temps.")
        .addComponent(searchBranchName)
        .addComponent(askBeforeOpening)
        .addComponent(openWithoutTicket)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();

    reset();
    return panel;
  }

  private void updateEnabledFields() {
    customTeam.setEnabled("AUTRE".equals(selected(team)));
    browserPath.setEnabled("custom".equals(selected(browser)));
  }

  @Override
  public boolean isModified() {
    TimmiSettings.State s = TimmiSettings.getInstance().getState();
    return enabled.isSelected() != s.enabled
        || !Objects.equals(selected(team), s.team)
        || !customTeam.getText().equals(s.customTeam)
        || !Objects.equals(selected(browser), s.browser)
        || !browserPath.getText().equals(s.browserPath)
        || !url.getText().equals(s.url)
        || searchBranchName.isSelected() != s.searchBranchName
        || askBeforeOpening.isSelected() != s.askBeforeOpening
        || openWithoutTicket.isSelected() != s.openWithoutTicket;
  }

  @Override
  public void apply() {
    TimmiSettings.State s = TimmiSettings.getInstance().getState();
    s.enabled = enabled.isSelected();
    s.team = selected(team);
    s.customTeam = customTeam.getText();
    s.browser = selected(browser);
    s.browserPath = browserPath.getText();
    s.url = url.getText();
    s.searchBranchName = searchBranchName.isSelected();
    s.askBeforeOpening = askBeforeOpening.isSelected();
    s.openWithoutTicket = openWithoutTicket.isSelected();
  }

  @Override
  public void reset() {
    TimmiSettings.State s = TimmiSettings.getInstance().getState();
    enabled.setSelected(s.enabled);
    select(team, TEAMS, s.team);
    customTeam.setText(s.customTeam);
    select(browser, BROWSERS, s.browser);
    browserPath.setText(s.browserPath);
    url.setText(s.url);
    searchBranchName.setSelected(s.searchBranchName);
    askBeforeOpening.setSelected(s.askBeforeOpening);
    openWithoutTicket.setSelected(s.openWithoutTicket);
    updateEnabledFields();
  }

  private static String selected(ComboBox<Choice> combo) {
    Choice choice = (Choice) combo.getSelectedItem();
    return choice == null ? null : choice.value();
  }

  private static void select(ComboBox<Choice> combo, Choice[] choices, String value) {
    for (Choice choice : choices) {
      if (choice.value().equals(value)) {
        combo.setSelectedItem(choice);
        return;
      }
    }
    combo.setSelectedIndex(0);
  }
}
