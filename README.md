# Timmi Commit

Après chaque commit, récupère le numéro de ticket Jira de votre équipe (ex. `EPSILON-123456`) dans le message de commit — ou à défaut dans le nom de la branche —, le copie dans le presse-papier et ouvre la page de saisie des temps Timmi : <https://safti.ilucca.net/timmi-timesheet/submission>.

Il ne reste plus qu'à coller (`Ctrl+V`) le ticket dans Timmi.

Les commits faits depuis l'IDE **et** depuis un terminal sont détectés. Les checkouts, pulls, rebases et `--amend` ne déclenchent pas l'ouverture.

## Éditeurs supportés

| Dossier | Éditeur | Paquet | Documentation |
| --- | --- | --- | --- |
| [`vscode/`](vscode/) | VS Code 1.80+ | `timmi-commit-<version>.vsix` | [vscode/README.md](vscode/README.md) |
| [`phpstorm/`](phpstorm/) | PhpStorm 2024.3+ (et autres IDE JetBrains avec Git) | `timmi-commit-<version>.zip` | [phpstorm/README.md](phpstorm/README.md) |

Les deux versions ont le même comportement et les mêmes réglages.

## Installation rapide

**VS Code**

```bash
cd vscode
npx @vscode/vsce package --allow-missing-repository
code --install-extension timmi-commit-0.1.1.vsix
```

**PhpStorm**

```bash
cd phpstorm
./gradlew buildPlugin
```

Puis dans PhpStorm : **Settings** → **Plugins** → roue dentée → **Install Plugin from Disk…** → `phpstorm/build/distributions/timmi-commit-0.1.1.zip`.

## Réglages

| Réglage | Défaut | Description |
| --- | --- | --- |
| Équipe | `EPSILON` | `ALPHA`, `BETA`, `DELTA`, `EPSILON`, `GAMMA`, `LAMBDA`, `AUTRE` (champ libre) ou `TOUTES` (n'importe quel `XXX-1234`) |
| Équipe personnalisée | | Préfixe libre, utilisé si l'équipe vaut `AUTRE` |
| Navigateur | par défaut | Navigateur du système, Firefox, Chrome, Chromium, Edge, Brave ou personnalisé |
| Chemin du navigateur | | Exécutable du navigateur si personnalisé |
| URL | page Timmi | URL ouverte |
| Activé | oui | Active / désactive l'ouverture automatique |
| Chercher dans la branche | oui | Chercher le ticket dans le nom de branche si absent du message |
| Demander confirmation | non | Afficher une notification « Ouvrir Timmi » au lieu d'ouvrir directement |
| Ouvrir sans ticket | oui | Ouvrir Timmi même sans ticket trouvé |

Une commande manuelle « Timmi : copier le ticket du dernier commit et ouvrir la saisie » est aussi disponible (palette `Ctrl+Shift+P` dans VS Code, menu **Tools** ou `Ctrl+Shift+A` dans PhpStorm).

## Licence

MIT — voir [LICENSE](LICENSE).
