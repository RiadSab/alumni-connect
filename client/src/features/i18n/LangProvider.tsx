// Holds the current language (persisted to localStorage) and provides t().

import { useCallback, useMemo, useState, type ReactNode } from "react";
import { en, fr, type TKey } from "./dictionary";
import { LangContext, type Lang } from "./lang-context";

const DICTS = { en, fr };
const STORAGE_KEY = "lang";

function initialLang(): Lang {
  const saved = localStorage.getItem(STORAGE_KEY);
  // ponytail: default "en" for now; flip to "fr" once every page is translated.
  return saved === "fr" || saved === "en" ? saved : "en";
}

// French treats 0 and 1 as singular (0 jour, 1 jour); English only 1 (0 days).
function isSingular(lang: Lang, n: number): boolean {
  return lang === "fr" ? Math.abs(n) <= 1 : n === 1;
}

if (import.meta.env.DEV) {
  console.assert(isSingular("en", 1) && !isSingular("en", 0), "en plural rule");
  console.assert(
    isSingular("fr", 0) && isSingular("fr", 1) && !isSingular("fr", 2),
    "fr plural rule",
  );
}

export function LangProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(initialLang);

  const setLang = useCallback((next: Lang) => {
    localStorage.setItem(STORAGE_KEY, next);
    setLangState(next);
  }, []);

  const t = useCallback(
    (key: TKey, vars?: Record<string, string | number>) => {
      let out: string = DICTS[lang][key];
      if (vars) {
        for (const [name, value] of Object.entries(vars)) {
          out = out.replace(`{${name}}`, String(value));
        }
      }
      return out;
    },
    [lang],
  );

  const tn = useCallback(
    (n: number, one: TKey, other: TKey, vars?: Record<string, string | number>) =>
      t(isSingular(lang, n) ? one : other, { n, ...vars }),
    [lang, t],
  );

  const value = useMemo(() => ({ lang, setLang, t, tn }), [lang, setLang, t, tn]);
  return <LangContext.Provider value={value}>{children}</LangContext.Provider>;
}
