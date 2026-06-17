// The language context + hook. Kept separate from the provider component so fast
// refresh stays happy (a file may export only components, or only non-components).

import { createContext, useContext } from "react";
import type { TKey } from "./dictionary";

export type Lang = "en" | "fr";

export interface LangContextValue {
  lang: Lang;
  setLang: (lang: Lang) => void;
  // Look up a UI string in the current language. `vars` fills {placeholders}.
  t: (key: TKey, vars?: Record<string, string | number>) => string;
}

export const LangContext = createContext<LangContextValue | null>(null);

export function useT(): LangContextValue {
  const ctx = useContext(LangContext);
  if (!ctx) throw new Error("useT must be used within <LangProvider>");
  return ctx;
}
