// UI string dictionaries. English is the source of truth: `fr` is typed as
// Record<TKey, string>, so every English key MUST have a French translation or
// it's a compile error (no silent missing-string fallback). Keys are added per
// surface as pages get translated — start small, grow as needed.

export const en = {
  "nav.jobs": "Jobs",
  "nav.dashboard": "Dashboard",
  "nav.login": "Login",
  "nav.logout": "Log out",
} satisfies Record<string, string>;

export type TKey = keyof typeof en;

export const fr: Record<TKey, string> = {
  "nav.jobs": "Offres",
  "nav.dashboard": "Tableau de bord",
  "nav.login": "Connexion",
  "nav.logout": "Déconnexion",
};
