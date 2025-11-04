import Container from "@/components/layout/Container";
import GlobalFilters from "@/components/filters/GlobalFilters";

export default function OverviewHero() {
  return (
    <section className="relative overflow-hidden">
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-zinc-100 to-transparent dark:from-zinc-900/50" />
      <Container>
        <div className="relative mx-auto max-w-screen-2xl px-6 py-8 md:py-10">
          <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
            <div className="space-y-2">
              <h2 className="text-2xl md:text-3xl font-semibold tracking-tight">
                Overview
              </h2>
              <p className="text-sm text-zinc-500">核心指标、一目了然</p>
            </div>
            <div className="md:pb-1">
              <GlobalFilters />
            </div>
          </div>
        </div>
      </Container>
    </section>
  );
}
