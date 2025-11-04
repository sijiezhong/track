import Header from "./Header";
import Sidebar from "./Sidebar";
import { Toaster } from "sonner";
import { useLocation } from "react-router-dom";

export default function PageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { pathname } = useLocation();
  const isOverview = pathname === "/" || pathname === "/overview";
  return (
    <div className="min-h-screen flex bg-background text-foreground">
      {!isOverview && <Sidebar />}
      <div className="flex-1 flex flex-col">
        {!isOverview && <Header />}
        <main className={isOverview ? "px-6 pb-8" : "p-6"}>{children}</main>
        <Toaster richColors position="top-right" />
      </div>
    </div>
  );
}
