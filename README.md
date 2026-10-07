# Timmi Commit

Extension VS Code qui, après chaque commit :

1. récupère le numéro de ticket Jira de votre équipe dans le message de commit (ex. `EPSILON-123456`) — ou à défaut dans le nom de la branche ;
2. le copie dans le presse-papier ;
3. ouvre la page de saisie des temps Timmi : <https://safti.ilucca.net/timmi-timesheet/submission>.

Il ne reste plus qu'à coller (`Ctrl+V`) le ticket dans Timmi.

Les commits faits depuis l'interface Git de VS Code **et** depuis le terminal intégré ou externe sont détectés (surveillance du `HEAD` via l'extension Git de VS Code). Les checkouts, pulls, rebases et `--amend` ne déclenchent pas l'ouverture.

## Commande

- **Timmi : copier le ticket du dernier commit et ouvrir la saisie** (palette `Ctrl+Shift+P`) : fait la même chose à la demande, pour le dernier commit.

## Configuration

| Réglage | Défaut | Description |
| --- | --- | --- |
| `timmiCommit.team` | `EPSILON` | `ALPHA`, `BETA`, `DELTA`, `EPSILON`, `GAMMA`, `LAMBDA`, `AUTRE` (champ libre) ou `TOUTES` (n'importe quel `XXX-1234`) |
| `timmiCommit.customTeam` | | Préfixe libre, utilisé si l'équipe vaut `AUTRE` |
| `timmiCommit.browser` | `default` | `default` (navigateur du système), `firefox`, `chrome`, `chromium`, `edge`, `brave`, `custom` |
| `timmiCommit.browserPath` | | Exécutable du navigateur si `custom` |
| `timmiCommit.url` | page Timmi | URL ouverte |
| `timmiCommit.enabled` | `true` | Active / désactive l'ouverture automatique |
| `timmiCommit.searchBranchName` | `true` | Chercher le ticket dans le nom de branche si absent du message |
| `timmiCommit.askBeforeOpening` | `false` | Afficher une notification « Ouvrir Timmi » au lieu d'ouvrir directement |
| `timmiCommit.openWithoutTicket` | `true` | Ouvrir Timmi même sans ticket trouvé |

## Prérequis

- VS Code 1.80 ou plus récent, avec la commande `code` dans le `PATH`
- Node.js 18 ou plus récent et `npm` (uniquement pour compiler)

## Compiler l'extension

L'extension est en JavaScript pur, sans dépendance : « compiler », c'est seulement créer le paquet `.vsix` avec l'outil officiel `vsce`.

```bash
cd timmi-lucca
npx @vscode/vsce package --allow-missing-repository
```

Le fichier `timmi-commit-<version>.vsix` est créé dans le dossier (par exemple `timmi-commit-0.1.1.vsix`).

Si tu modifies l'extension, augmente `version` dans `package.json` avant de recompiler.

## Installer l'extension

En ligne de commande :

```bash
code --install-extension timmi-commit-0.1.1.vsix
```

Ou depuis VS Code : vue **Extensions** (`Ctrl+Shift+X`) → menu `…` en haut → **Installer à partir d'un VSIX…** → choisir le fichier `.vsix`.

Pour mettre à jour, réinstalle le nouveau `.vsix` de la même façon. Si VS Code le demande, recharge la fenêtre (`Ctrl+Shift+P` → **Developer: Reload Window**).

Pour vérifier que l'extension est installée :

```bash
code --list-extensions | grep timmi-commit
```

## Désinstaller l'extension

En ligne de commande :

```bash
code --uninstall-extension safti.timmi-commit
```

Ou depuis VS Code : vue **Extensions** → rechercher « Timmi Commit » → roue dentée → **Désinstaller**.

## Développer

Ouvre ce dossier dans VS Code et appuie sur `F5`. Une seconde fenêtre VS Code (Extension Development Host) s'ouvre avec l'extension chargée. Les logs sont dans le panneau **Sortie** → canal « Timmi Commit ».
