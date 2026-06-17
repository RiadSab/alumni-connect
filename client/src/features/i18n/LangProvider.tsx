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

  const value = useMemo(() => ({ lang, setLang, t }), [lang, setLang, t]);
  return <LangContext.Provider value={value}>{children}</LangContext.Provider>;
}
