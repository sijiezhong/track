import { Skeleton } from "@/components/ui/skeleton";

type ChartContainerProps = {
  isLoading?: boolean;
  isEmpty?: boolean;
  error?: string | null;
  minHeight?: number;
  children: React.ReactNode;
};

export default function ChartContainer({
  isLoading,
  isEmpty,
  error,
  minHeight = 320,
  children,
}: ChartContainerProps) {
  if (isLoading) return <Skeleton className={`h-[${minHeight}px]`} />;
  if (error)
    return (
      <div
        className="flex h-full items-center justify-center text-sm text-red-500"
        style={{ minHeight }}
      >
        {error}
      </div>
    );
  if (isEmpty)
    return (
      <div
        className="flex h-full items-center justify-center text-sm text-zinc-500"
        style={{ minHeight }}
      >
        暂无数据
      </div>
    );
  return <div style={{ minHeight }}>{children}</div>;
}
