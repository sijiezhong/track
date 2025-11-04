import { Link, useLocation } from "react-router-dom";

const nav = [
  { to: "/overview", label: "概览 Overview" },
  { to: "/analytics/trends", label: "趋势分析" },
  { to: "/analytics/pages", label: "页面分析" },
  { to: "/analytics/custom", label: "自定义事件" },
  { to: "/analytics/performance", label: "性能分析" },
  { to: "/analytics/errors", label: "错误分析" },
  { to: "/user/behavior", label: "用户行为路径" },
  { to: "/events", label: "事件明细" },
];

export default function Sidebar() {
  const { pathname } = useLocation();
  return (
    <aside className="w-56 border-r min-h-screen bg-white dark:bg-zinc-950">
      <nav className="p-4 space-y-1">
        {nav.map((n) => {
          const active = pathname === n.to;
          return (
            <Link
              key={n.to}
              to={n.to}
              className={
                "relative block rounded px-3 py-2 text-sm transition-colors " +
                (active
                  ? "bg-zinc-100 dark:bg-zinc-800 font-medium before:content-[''] before:absolute before:left-0 before:top-0 before:h-full before:w-0.5 before:bg-zinc-900 dark:before:bg-zinc-100"
                  : "hover:bg-zinc-50 dark:hover:bg-zinc-900")
              }
            >
              {n.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
