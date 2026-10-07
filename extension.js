'use strict';

const vscode = require('vscode');
const { spawn } = require('child_process');

const CONFIG_SECTION = 'timmiCommit';

// Un commit plus vieux que ça n'est pas considéré comme « nouveau » (pull, reset…).
const MAX_COMMIT_AGE_MS = 5 * 60 * 1000;

// Commandes / applications candidates par navigateur et par plateforme.
const BROWSERS = {
  linux: {
    firefox: ['firefox', 'firefox-esr'],
    chrome: ['google-chrome', 'google-chrome-stable'],
    chromium: ['chromium', 'chromium-browser'],
    edge: ['microsoft-edge', 'microsoft-edge-stable'],
    brave: ['brave-browser', 'brave']
  },
  darwin: {
    firefox: ['Firefox'],
    chrome: ['Google Chrome'],
    chromium: ['Chromium'],
    edge: ['Microsoft Edge'],
    brave: ['Brave Browser']
  },
  win32: {
    firefox: ['firefox'],
    chrome: ['chrome'],
    chromium: ['chromium'],
    edge: ['msedge'],
    brave: ['brave']
  }
};

/** @type {vscode.OutputChannel} */
let output;
const handledCommits = new Set();

/**
 * @param {vscode.ExtensionContext} context
 */
async function activate(context) {
  output = vscode.window.createOutputChannel('Timmi Commit');
  context.subscriptions.push(output);

  context.subscriptions.push(
    vscode.commands.registerCommand('timmiCommit.openTimmi', () => openForLastCommit())
  );

  const git = await getGitApi();
  if (!git) {
    log("Extension Git de VS Code indisponible : détection des commits désactivée.");
    return;
  }

  git.repositories.forEach(repo => watchRepository(repo, context));
  context.subscriptions.push(git.onDidOpenRepository(repo => watchRepository(repo, context)));
}

function deactivate() {}

async function getGitApi() {
  const ext = vscode.extensions.getExtension('vscode.git');
  if (!ext) {
    return undefined;
  }
  const exports = ext.isActive ? ext.exports : await ext.activate();
  return exports.getAPI(1);
}

/**
 * Surveille le HEAD d'un dépôt : un nouveau commit sur la même branche, dont le parent
 * est l'ancien HEAD, est considéré comme un commit local (UI VS Code ou terminal).
 */
function watchRepository(repo, context) {
  let lastCommit = repo.state.HEAD && repo.state.HEAD.commit;
  let lastBranch = repo.state.HEAD && repo.state.HEAD.name;
  log(`Surveillance du dépôt ${repo.rootUri.fsPath}`);

  const disposable = repo.state.onDidChange(async () => {
    const head = repo.state.HEAD;
    if (!head || !head.commit || head.commit === lastCommit) {
      return;
    }
    const previousCommit = lastCommit;
    const previousBranch = lastBranch;
    lastCommit = head.commit;
    lastBranch = head.name;

    if (!previousCommit || head.name !== previousBranch || handledCommits.has(head.commit)) {
      return;
    }

    try {
      const commit = await repo.getCommit(head.commit);
      if (!(await isNewLocalCommit(repo, commit, previousCommit))) {
        return;
      }
      handledCommits.add(commit.hash);
      await handleCommit(commit.message, head.name);
    } catch (err) {
      log(`Erreur lors de l'analyse du commit ${head.commit} : ${err}`);
    }
  });
  context.subscriptions.push(disposable);
}

async function isNewLocalCommit(repo, commit, previousCommit) {
  if (!commit.parents || !commit.parents.includes(previousCommit)) {
    return false;
  }
  const date = commit.commitDate || commit.authorDate;
  if (date && Date.now() - new Date(date).getTime() > MAX_COMMIT_AGE_MS) {
    return false;
  }
  try {
    const email = await repo.getConfig('user.email');
    if (email && commit.authorEmail && email.toLowerCase() !== commit.authorEmail.toLowerCase()) {
      return false;
    }
  } catch {
    // Pas d'e-mail configuré : on ne filtre pas sur l'auteur.
  }
  return true;
}

async function openForLastCommit() {
  const git = await getGitApi();
  const repo = git && pickRepository(git);
  if (!repo || !repo.state.HEAD || !repo.state.HEAD.commit) {
    await handleCommit('', undefined, true);
    return;
  }
  const commit = await repo.getCommit(repo.state.HEAD.commit);
  await handleCommit(commit.message, repo.state.HEAD.name, true);
}

function pickRepository(git) {
  if (git.repositories.length <= 1) {
    return git.repositories[0];
  }
  const editor = vscode.window.activeTextEditor;
  if (editor) {
    const repo = git.getRepository(editor.document.uri);
    if (repo) {
      return repo;
    }
  }
  return git.repositories[0];
}

/**
 * @param {string} message message du commit
 * @param {string | undefined} branch nom de la branche courante
 * @param {boolean} manual déclenché par la commande (ignore le réglage « enabled »)
 */
async function handleCommit(message, branch, manual = false) {
  const config = vscode.workspace.getConfiguration(CONFIG_SECTION);
  if (!manual && !config.get('enabled', true)) {
    return;
  }

  const regex = buildTicketRegex(config);
  let ticket = findTicket(message, regex);
  if (!ticket && branch && config.get('searchBranchName', true)) {
    ticket = findTicket(branch, regex);
  }

  if (ticket) {
    await vscode.env.clipboard.writeText(ticket);
    log(`Ticket ${ticket} copié dans le presse-papier.`);
  } else {
    log(`Aucun ticket trouvé dans « ${firstLine(message)} ».`);
    if (!manual && !config.get('openWithoutTicket', true)) {
      return;
    }
  }

  const status = ticket
    ? `Ticket ${ticket} copié dans le presse-papier.`
    : 'Aucun numéro de ticket trouvé dans le commit.';

  if (config.get('askBeforeOpening', false) && !manual) {
    const choice = await vscode.window.showInformationMessage(status, 'Ouvrir Timmi');
    if (choice === 'Ouvrir Timmi') {
      await openBrowser(config);
    }
    return;
  }

  vscode.window.showInformationMessage(`${status} Ouverture de Timmi…`);
  await openBrowser(config);
}

function buildTicketRegex(config) {
  let team = config.get('team', 'EPSILON');
  if (team === 'AUTRE') {
    team = (config.get('customTeam', '') || '').trim();
  }
  if (!team || team === 'TOUTES') {
    return /\b[A-Z][A-Z0-9_]+-\d+\b/i;
  }
  const escaped = team.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`\\b${escaped}-\\d+\\b`, 'i');
}

function findTicket(text, regex) {
  const match = (text || '').match(regex);
  return match ? match[0].toUpperCase() : undefined;
}

function firstLine(text) {
  return (text || '').split('\n')[0];
}

async function openBrowser(config) {
  const url = config.get('url', 'https://safti.ilucca.net/timmi-timesheet/submission');
  const browser = config.get('browser', 'default');

  if (browser !== 'default') {
    const opened = browser === 'custom'
      ? await launchCustom((config.get('browserPath', '') || '').trim(), url)
      : await launchKnown(browser, url);
    if (opened) {
      return;
    }
    vscode.window.showWarningMessage(
      `Impossible de lancer le navigateur « ${browser} » : ouverture avec le navigateur par défaut.`
    );
  }

  await vscode.env.openExternal(vscode.Uri.parse(url));
}

async function launchKnown(browser, url) {
  const candidates = (BROWSERS[process.platform] || BROWSERS.linux)[browser] || [];
  for (const candidate of candidates) {
    let ok;
    if (process.platform === 'darwin') {
      ok = await runToCompletion('open', ['-a', candidate, url]);
    } else if (process.platform === 'win32') {
      ok = await runToCompletion('cmd', ['/c', 'start', '""', candidate, url]);
    } else {
      ok = await launchDetached(candidate, [url]);
    }
    if (ok) {
      log(`Timmi ouvert avec ${candidate}.`);
      return true;
    }
  }
  return false;
}

async function launchCustom(path, url) {
  if (!path) {
    log('Navigateur « custom » sélectionné mais aucun chemin configuré.');
    return false;
  }
  if (process.platform === 'darwin' && path.endsWith('.app')) {
    return runToCompletion('open', ['-a', path, url]);
  }
  return launchDetached(path, [url]);
}

/** Lance un processus détaché ; résout true dès qu'il a démarré. */
function launchDetached(command, args) {
  return new Promise(resolve => {
    try {
      const child = spawn(command, args, { detached: true, stdio: 'ignore' });
      child.once('error', err => {
        log(`Échec du lancement de ${command} : ${err.message}`);
        resolve(false);
      });
      child.once('spawn', () => {
        child.unref();
        resolve(true);
      });
    } catch (err) {
      log(`Échec du lancement de ${command} : ${err}`);
      resolve(false);
    }
  });
}

/** Lance une commande courte (open / start) ; résout true si elle se termine avec le code 0. */
function runToCompletion(command, args) {
  return new Promise(resolve => {
    try {
      const child = spawn(command, args, { stdio: 'ignore', windowsVerbatimArguments: true });
      child.once('error', () => resolve(false));
      child.once('exit', code => resolve(code === 0));
    } catch {
      resolve(false);
    }
  });
}

function log(message) {
  if (output) {
    output.appendLine(`[${new Date().toLocaleTimeString()}] ${message}`);
  }
}

module.exports = { activate, deactivate, buildTicketRegex, findTicket };
