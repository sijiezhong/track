import { useEffect, useMemo, useState } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Info } from "lucide-react";

type Term = { term: string; definition: React.ReactNode };

type PageIntroProps = {
  storageKey?: string;
  title: string;
  description: React.ReactNode;
  terms?: Term[];
  defaultVisible?: boolean;
};

export default function PageIntro({
  storageKey,
  title,
  description,
  terms,
  defaultVisible = true,
}: PageIntroProps) {
  const [visible, setVisible] = useState(defaultVisible);
  const [showTerms, setShowTerms] = useState(true);
  const key = useMemo(
    () => (storageKey ? `intro.hidden:${storageKey}` : undefined),
    [storageKey],
  );

  useEffect(() => {
    if (!key) return;
    const hidden =
      typeof window !== "undefined" ? localStorage.getItem(key) : null;
    if (hidden === "1") setVisible(false);
  }, [key]);

  const hide = () => {
    setVisible(false);
    if (key) localStorage.setItem(key, "1");
  };

  if (!visible) return null;

  return (
    <Alert className="flex items-start gap-3">
      <Info className="h-4 w-4 mt-0.5" />
      <div className="flex-1">
        <AlertTitle>{title}</AlertTitle>
        <AlertDescription>
          <div className="space-y-3">
            <div>{description}</div>
            {Array.isArray(terms) && terms.length > 0 && (
              <div className="text-xs">
                <button
                  type="button"
                  className="text-zinc-600 hover:text-zinc-900 underline underline-offset-4"
                  onClick={() => setShowTerms((v) => !v)}
                  aria-expanded={showTerms}
                >
                  {showTerms ? "收起名词解释" : "展开名词解释"}
                </button>
                {showTerms && (
                  <ul className="mt-2 grid gap-1 sm:grid-cols-2">
                    {terms.map((t) => (
                      <li key={t.term} className="leading-relaxed">
                        <span className="font-medium mr-2">{t.term}</span>
                        <span className="text-zinc-600">{t.definition}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        </AlertDescription>
      </div>
      {key && (
        <button
          aria-label="不再显示本页介绍"
          className="text-xs text-zinc-500 hover:text-zinc-900 underline underline-offset-4"
          onClick={hide}
        >
          不再显示
        </button>
      )}
    </Alert>
  );
}
