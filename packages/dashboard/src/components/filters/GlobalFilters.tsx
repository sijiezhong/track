import { useEffect, useState } from "react";
import { useFiltersStore } from "@/store/filters.store";
import { getProjects, type ProjectItem } from "@/services/projects";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Calendar } from "@/components/ui/calendar";
import type { DateRange } from "react-day-picker";

export default function GlobalFilters() {
  const { appId, setAppId, start, end, setRange, triggerRefresh } =
    useFiltersStore();
  const [projects, setProjects] = useState<ProjectItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    getProjects({ active: true })
      .then((list) => {
        if (!mounted) return;
        setProjects(list);
        // 如果项目列表不为空且当前没有选择项目，自动选择第一项
        if (list.length > 0 && !appId) {
          setAppId(list[0].appId);
        }
      })
      .finally(() => {
        if (!mounted) return;
        setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [appId, setAppId]);

  const toInput = (d?: Date | null) => {
    if (!d) return "";
    const pad = (n: number) => String(n).padStart(2, "0");
    const yyyy = d.getFullYear();
    const MM = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    const hh = pad(d.getHours());
    const mm = pad(d.getMinutes());
    return `${yyyy}-${MM}-${dd}T${hh}:${mm}`;
  };

  const toDateOnly = (d?: Date | null) => {
    if (!d) return "";
    const pad = (n: number) => String(n).padStart(2, "0");
    const yyyy = d.getFullYear();
    const MM = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    return `${yyyy}-${MM}-${dd}`;
  };

  const [range, setRangeState] = useState<DateRange | undefined>({
    from: start ? new Date(start) : undefined,
    to: end ? new Date(end) : undefined,
  });

  useEffect(() => {
    setRange(toInput(range?.from ?? null), toInput(range?.to ?? null));
  }, [range?.from, range?.to]);

  return (
    <div className="flex items-center gap-2">
      <div className="flex items-center gap-2">
        <Label className="text-xs text-zinc-500">项目</Label>
        <Select value={appId || ""} onValueChange={setAppId} required>
          <SelectTrigger className="w-[220px]">
            <SelectValue
              placeholder={
                loading
                  ? "加载中…"
                  : projects.length === 0
                    ? "暂无项目"
                    : "请选择项目（必填）"
              }
            />
          </SelectTrigger>
          <SelectContent>
            {projects.map((p) => (
              <SelectItem key={p.appId} value={p.appId}>
                {p.appName}（{p.appId}）
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {!appId && projects.length > 0 && (
          <span className="text-xs text-red-500">请选择项目</span>
        )}
      </div>

      <div className="flex items-center gap-2">
        <Label className="text-xs text-zinc-500">时间范围</Label>
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              className="w-[260px] justify-start"
            >
              {range?.from && range?.to
                ? `${toDateOnly(range.from)} — ${toDateOnly(range.to)}`
                : "选择时间范围"}
            </Button>
          </PopoverTrigger>
          <PopoverContent
            side="bottom"
            align="start"
            sideOffset={6}
            className="p-0 w-auto max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-auto"
          >
            <Calendar
              mode="range"
              selected={range}
              onSelect={(r) => setRangeState(r)}
              numberOfMonths={1}
            />
          </PopoverContent>
        </Popover>
      </div>

      {/* 时区选择移除：后端默认处理为 UTC/或服务端口径，前端不传 timezone */}

      <Button variant="outline" size="sm" onClick={triggerRefresh}>
        刷新
      </Button>
    </div>
  );
}
