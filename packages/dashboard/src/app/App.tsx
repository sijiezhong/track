import { createBrowserRouter, RouterProvider } from "react-router-dom";
import Overview from "@/pages/Overview";
import AnalyticsTrends from "@/pages/AnalyticsTrends";
import AnalyticsPages from "@/pages/AnalyticsPages";
import AnalyticsCustom from "@/pages/AnalyticsCustom";
import AnalyticsPerformance from "@/pages/AnalyticsPerformance";
import AnalyticsErrors from "@/pages/AnalyticsErrors";
import UserBehavior from "@/pages/UserBehavior";
import Events from "@/pages/Events";
import PageLayout from "@/components/layout/Page";

const withLayout = (el: JSX.Element) => <PageLayout>{el}</PageLayout>;

const router = createBrowserRouter(
  [
    { path: "/", element: withLayout(<Overview />) },
    { path: "/overview", element: withLayout(<Overview />) },
    { path: "/analytics/trends", element: withLayout(<AnalyticsTrends />) },
    { path: "/analytics/pages", element: withLayout(<AnalyticsPages />) },
    { path: "/analytics/custom", element: withLayout(<AnalyticsCustom />) },
    {
      path: "/analytics/performance",
      element: withLayout(<AnalyticsPerformance />),
    },
    { path: "/analytics/errors", element: withLayout(<AnalyticsErrors />) },
    { path: "/user/behavior", element: withLayout(<UserBehavior />) },
    { path: "/events", element: withLayout(<Events />) },
  ],
  {
    future: {
      v7_startTransition: true,
    },
  },
);

export default function App() {
  return (
    <RouterProvider router={router} future={{ v7_startTransition: true }} />
  );
}
