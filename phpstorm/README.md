# Timmi Commit — PhpStorm

Plugin PhpStorm (et autres IDE JetBrains) équivalent à l'extension VS Code du dossier parent. Après chaque commit :

1. récupère le numéro de ticket Jira de votre équipe dans le message de commit (ex. `EPSILON-123456`) — ou à défaut dans le nom de la branche ;
2. le copie dans le presse-papier ;
3. ouvre la page de saisie des temps Timmi : <https://safti.ilucca.net/timmi-timesheet/submission>.

Il ne reste plus qu'à coller (`Ctrl+V`) le ticket dans Timmi.

Les commits faits depuis la fenêtre de commit de PhpStorm **et** depuis le terminal intégré ou externe sont détectés (surveillance du `HEAD` via le plugin Git de l'IDE). Les checkouts, pulls, rebases et `--amend` ne déclenchent pas l'ouverture.

## Action

- **Timmi : copier le ticket du dernier commit et ouvrir la saisie** (menu **Tools**, ou `Ctrl+Shift+A` puis taper « Timmi ») : fait la même chose à la demande, pour le dernier commit.

## Configuration

**Settings** → **Tools** → **Timmi Commit**. Les réglages sont globaux (communs à tous les projets).

| Réglage | Défaut | Description |
| --- | --- | --- |
| Équipe | `EPSILON` | `ALPHA`, `BETA`, `DELTA`, `EPSILON`, `GAMMA`, `LAMBDA`, `AUTRE` (champ libre) ou `TOUTES` (n'importe quel `XXX-1234`) |
| Équipe personnalisée | | Préfixe libre, utilisé si l'équipe vaut `AUTRE` |
| Navigateur | par défaut | Navigateur du système, Firefox, Chrome, Chromium, Edge, Brave ou Personnalisé |
| Chemin du navigateur | | Exécutable du navigateur si Personnalisé |
| URL | page Timmi | URL ouverte |
| Activer l'ouverture… | oui | Active / désactive l'ouverture automatique |
| Chercher dans le nom de la branche | oui | Chercher le ticket dans le nom de branche si absent du message |
| Demander confirmation | non | Afficher une notification « Ouvrir Timmi » au lieu d'ouvrir directement |
| Ouvrir sans ticket | oui | Ouvrir Timmi même sans ticket trouvé |

## Prérequis

- PhpStorm 2024.3 ou plus récent, avec le plugin Git activé (c'est le cas par défaut)
- Pour compiler : un accès Internet. Gradle est fourni (`gradlew`) et un JDK 21 est téléchargé automatiquement si besoin.

## Compiler le plugin

```bash
cd timmi-lucca/phpstorm
./gradlew buildPlugin
```

Le premier build télécharge PhpStorm (pour compiler contre son API), il prend donc quelques minutes. Le fichier `build/distributions/timmi-commit-<version>.zip` est créé (par exemple `timmi-commit-0.1.1.zip`).

Si tu modifies le plugin, augmente `pluginVersion` dans `gradle.properties` avant de recompiler.

## Installer le plugin

Dans PhpStorm : **Settings** → **Plugins** → roue dentée en haut → **Install Plugin from Disk…** → choisir le fichier `.zip` (ne pas le décompresser), puis redémarrer l'IDE si demandé.

Pour mettre à jour, réinstalle le nouveau `.zip` de la même façon.

## Désinstaller le plugin

**Settings** → **Plugins** → onglet **Installed** → « Timmi Commit » → flèche à côté de **Disable** → **Uninstall**.

## Développer

```bash
./gradlew runIde
```

Une instance PhpStorm de test s'ouvre avec le plugin chargé. Les logs sont dans `idea.log` (**Help** → **Show Log in Files**), préfixés par `Timmi Commit`.

## Licence

MIT — voir le fichier [`LICENSE`](../vscode/LICENSE).
