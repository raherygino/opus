import { create } from "zustand";
import type { MapViewport, MapMarker, MapRoute, MapPolygon, ActiveLayer } from "@/types/map";
export type { ActiveLayer };

interface MapState {
  viewport: MapViewport;
  activeLayer: ActiveLayer;
  markers: MapMarker[];
  routes: MapRoute[];
  polygons: MapPolygon[];
  selectedFeatureId: string | null;
  isFullscreen: boolean;
  showToolsPanel: boolean;
  isLoading: boolean;
  error: string | null;

  setViewport: (viewport: Partial<MapViewport>) => void;
  setActiveLayer: (layer: ActiveLayer) => void;
  setMarkers: (markers: MapMarker[]) => void;
  addMarker: (marker: MapMarker) => void;
  removeMarker: (id: string) => void;
  setRoutes: (routes: MapRoute[]) => void;
  addRoute: (route: MapRoute) => void;
  removeRoute: (id: string) => void;
  setPolygons: (polygons: MapPolygon[]) => void;
  addPolygon: (polygon: MapPolygon) => void;
  removePolygon: (id: string) => void;
  selectFeature: (id: string | null) => void;
  setFullscreen: (fullscreen: boolean) => void;
  toggleToolsPanel: () => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  reset: () => void;
}

const initialState = {
  viewport: {
    center: [48.8566, 2.3522] as [number, number],
    zoom: 13,
  },
  activeLayer: "streets" as ActiveLayer,
  markers: [] as MapMarker[],
  routes: [] as MapRoute[],
  polygons: [] as MapPolygon[],
  selectedFeatureId: null,
  isFullscreen: false,
  showToolsPanel: true,
  isLoading: false,
  error: null,
};

export const useMapStore = create<MapState>((set) => ({
  ...initialState,

  setViewport: (viewport) =>
    set((state) => ({
      viewport: { ...state.viewport, ...viewport },
    })),

  setActiveLayer: (layer) => set({ activeLayer: layer }),

  setMarkers: (markers) => set({ markers }),
  addMarker: (marker) =>
    set((state) => ({ markers: [...state.markers, marker] })),
  removeMarker: (id) =>
    set((state) => ({
      markers: state.markers.filter((m) => m.id !== id),
    })),

  setRoutes: (routes) => set({ routes }),
  addRoute: (route) =>
    set((state) => ({ routes: [...state.routes, route] })),
  removeRoute: (id) =>
    set((state) => ({
      routes: state.routes.filter((r) => r.id !== id),
    })),

  setPolygons: (polygons) => set({ polygons }),
  addPolygon: (polygon) =>
    set((state) => ({ polygons: [...state.polygons, polygon] })),
  removePolygon: (id) =>
    set((state) => ({
      polygons: state.polygons.filter((p) => p.id !== id),
    })),

  selectFeature: (id) => set({ selectedFeatureId: id }),
  setFullscreen: (fullscreen) => set({ isFullscreen: fullscreen }),
  toggleToolsPanel: () =>
    set((state) => ({ showToolsPanel: !state.showToolsPanel })),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error }),

  reset: () => set(initialState),
}));
