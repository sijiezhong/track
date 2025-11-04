import { create } from "zustand";

type FiltersState = {
  appId: string;
  start: string | null;
  end: string | null;
  refreshKey: number;
  setAppId: (v: string) => void;
  setRange: (start: string | null, end: string | null) => void;
  triggerRefresh: () => void;
};

export const useFiltersStore = create<FiltersState>((set) => ({
  appId: "",
  start: null,
  end: null,
  refreshKey: 0,
  setAppId: (v) => set({ appId: v }),
  setRange: (start, end) => set({ start, end }),
  triggerRefresh: () => set((s) => ({ refreshKey: s.refreshKey + 1 })),
}));
