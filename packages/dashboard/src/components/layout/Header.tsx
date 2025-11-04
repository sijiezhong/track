import GlobalFilters from "@/components/filters/GlobalFilters";
import Container from "@/components/layout/Container";

export default function Header({
  transparent = false,
}: {
  transparent?: boolean;
}) {
  return (
    <header
      className={
        transparent
          ? "sticky top-0 z-40 border-b border-transparent bg-transparent"
          : "sticky top-0 z-40 border-b backdrop-blur supports-[backdrop-filter]:bg-white/70 dark:supports-[backdrop-filter]:bg-zinc-900/70 shadow-sm"
      }
    >
      <Container>
        <div className="py-3 flex items-center justify-between gap-4">
          <h1 className="text-xl font-semibold">Track Dashboard</h1>
          <GlobalFilters />
        </div>
      </Container>
    </header>
  );
}
