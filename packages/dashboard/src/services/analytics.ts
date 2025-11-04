import { api } from "./http";

export async function getOverview(params: {
  appId?: string;
  start?: string;
  end?: string;
}) {
  const { data } = await api.get("/api/analytics/overview", { params });
  return data as {
    pv: number;
    uv: number;
    bounceRate: number;
    avgDurationSec: number;
    timezone: string;
  };
}

export async function getPVUVSeries(params: {
  appId?: string;
  start: string;
  end: string;
  interval: "minute" | "hour" | "day";
}) {
  const { data } = await api.get("/api/analytics/pv-uv/series", { params });
  return data as {
    series: { ts: string; pv: number; uv: number }[];
    interval: string;
    timezone: string;
  };
}

export async function getPagesTop(params: {
  appId?: string;
  start: string;
  end: string;
  limit?: number;
}) {
  const { data } = await api.get("/api/analytics/pages/top", { params });
  return data as {
    list: { pageUrl: string; pv: number; uv: number; avgDurationSec: number }[];
    total: number;
  };
}

export async function getEventsDistribution(params: {
  appId?: string;
  start: string;
  end: string;
}) {
  const { data } = await api.get("/api/analytics/events-distribution", {
    params,
  });
  return data as { list: { type: string; value: number }[] };
}

export async function getWebVitals(params: {
  appId?: string;
  start: string;
  end: string;
  metric?: string;
}) {
  const { data } = await api.get("/api/analytics/web-vitals", { params });
  return data as { p50: number; p75: number; p95: number; unit: string };
}

export async function getCustomEvents(params: {
  appId?: string;
  eventId?: string;
  start: string;
  end: string;
  groupBy?: "hour" | "day";
}) {
  const { data } = await api.get("/api/analytics/custom-events", { params });
  return data as {
    series: { ts: string; count: number }[];
    total: number;
    groupBy: string;
  };
}

export async function getCustomEventsTop(params: {
  appId?: string;
  start: string;
  end: string;
  limit?: number;
}) {
  const { data } = await api.get("/api/analytics/custom-events/top", {
    params,
  });
  return data as { list: { eventId: string; count: number }[]; total: number };
}

export async function getWebVitalsSeries(params: {
  appId?: string;
  start: string;
  end: string;
  metric: string;
  interval?: "hour" | "day";
}) {
  const { data } = await api.get("/api/analytics/web-vitals/series", {
    params,
  });
  return data as {
    series: { ts: string; p50: number; p75: number; p95: number }[];
    interval: string;
    timezone: string;
  };
}

export async function getErrorsTrend(params: {
  appId?: string;
  start: string;
  end: string;
  interval?: "hour" | "day";
}) {
  const { data } = await api.get("/api/analytics/errors/trend", { params });
  return data as {
    series: { ts: string; count: number }[];
    interval: string;
    timezone: string;
  };
}

export async function getErrorsTop(params: {
  appId?: string;
  start: string;
  end: string;
  limit?: number;
}) {
  const { data } = await api.get("/api/analytics/errors/top", { params });
  return data as {
    list: {
      fingerprint: string;
      message: string;
      count: number;
      firstSeen: string;
      lastSeen: string;
    }[];
    total: number;
  };
}
