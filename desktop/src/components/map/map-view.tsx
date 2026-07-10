import { useEffect, useRef } from "react";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import "@/styles/map.css";
import { useMapStore } from "@/stores/map-store";
import type { ActiveLayer } from "@/stores/map-store";
import { useThemeStore } from "@/stores/theme-store";
import { getTheme } from "@/themes";
import type { ThemeId } from "@/types";
import { cn } from "@/lib/utils";
import { setMapInstance } from "./map-instance";
import { mapboxToken } from "@/config/map";
import { AlertTriangle } from "lucide-react";

const MAPBOX_STYLE = {
  streets: {
    light: "mapbox://styles/mapbox/streets-v12",
    dark: "mapbox://styles/mapbox/navigation-night-v1",
  },
  satellite: {
    light: "mapbox://styles/mapbox/satellite-v9",
    dark: "mapbox://styles/mapbox/satellite-streets-v12",
  },
} as const;

const LAYER_MAX_ZOOM: Record<ActiveLayer, number> = {
  streets: 19,
  satellite: 17,
};

const DEFAULT_PITCH = 45;

interface MapViewProps {
  className?: string;
}

function getStyleKey(themeId: string, activeLayer: ActiveLayer) {
  const isDark = getTheme(themeId as ThemeId).type === "dark";
  const themeKey = isDark ? "dark" : "light";
  return MAPBOX_STYLE[activeLayer][themeKey];
}

function add3DBuildings(map: mapboxgl.Map) {
  if (map.getLayer("3d-buildings")) return;

  const layers = map.getStyle()?.layers || [];
  const labelLayerId = layers.find(
    (l) => l.type === "symbol" && (l as mapboxgl.SymbolLayer).layout?.["text-field"],
  )?.id;

  try {
    map.addLayer(
      {
        id: "3d-buildings",
        source: "composite",
        "source-layer": "building",
        filter: ["==", "extrude", "true"],
        type: "fill-extrusion",
        minzoom: 15,
        paint: {
          "fill-extrusion-color": "#aaa",
          "fill-extrusion-height": ["get", "height"],
          "fill-extrusion-base": ["get", "min_height"],
          "fill-extrusion-opacity": 0.6,
        },
      },
      labelLayerId,
    );
  } catch {
    // Source or source-layer may not exist in some styles
  }
}

function NoTokenOverlay() {
  return (
    <div className="flex h-full w-full items-center justify-center rounded-lg border border-border bg-card">
      <div className="flex max-w-md flex-col items-center gap-3 text-center">
        <AlertTriangle className="h-8 w-8 text-amber-500" />
        <div>
          <h3 className="text-sm font-semibold text-foreground">Clé Mapbox manquante</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Ajoutez votre jeton Mapbox dans le fichier <code className="rounded bg-muted px-1 py-0.5 text-[10px]">.env</code> à la variable <code className="rounded bg-muted px-1 py-0.5 text-[10px]">VITE_MAPBOX_TOKEN</code>.
          </p>
        </div>
      </div>
    </div>
  );
}

export function MapView({ className }: MapViewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);

  const viewport = useMapStore((s) => s.viewport);
  const activeLayer = useMapStore((s) => s.activeLayer);
  const themeId = useThemeStore((s) => s.theme);
  const setViewport = useMapStore((s) => s.setViewport);

  // Keep latest style key in a ref so the style-load handler can access it
  const styleKeyRef = useRef(getStyleKey(themeId, activeLayer));
  styleKeyRef.current = getStyleKey(themeId, activeLayer);

  // Initialize the map once
  useEffect(() => {
    if (!containerRef.current || !mapboxToken) return;

    const maxZoom = LAYER_MAX_ZOOM[activeLayer];

    const map = new mapboxgl.Map({
      accessToken: mapboxToken,
      container: containerRef.current,
      style: styleKeyRef.current,
      center: [viewport.center[1], viewport.center[0]],
      zoom: Math.min(viewport.zoom, maxZoom),
      pitch: DEFAULT_PITCH,
      bearing: 0,
      maxZoom,
      attributionControl: true,
    });

    mapRef.current = map;
    setMapInstance(map);

    map.addControl(new mapboxgl.NavigationControl({ visualizePitch: true }), "bottom-right");
    map.addControl(new mapboxgl.ScaleControl({ unit: "metric" }), "bottom-left");

    // Re-add 3D buildings every time the style loads (including after setStyle)
    const onStyleLoad = () => {
      add3DBuildings(map);
    };
    map.on("style.load", onStyleLoad);

    // Sync viewport to the store
    const sync = () => {
      const center = map.getCenter();
      setViewport({ center: [center.lat, center.lng], zoom: map.getZoom() });
    };
    map.on("moveend", sync);

    return () => {
      map.off("moveend", sync);
      map.off("style.load", onStyleLoad);
      map.remove();
      mapRef.current = null;
      setMapInstance(null);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update style when layer or theme changes
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    const newStyle = getStyleKey(themeId, activeLayer);
    const maxZoom = LAYER_MAX_ZOOM[activeLayer];

    map.setMaxZoom(maxZoom);
    map.setStyle(newStyle);
  }, [activeLayer, themeId]);

  if (!mapboxToken) {
    return (
      <div className={cn("relative h-full w-full overflow-hidden rounded-lg border border-border", className)}>
        <NoTokenOverlay />
      </div>
    );
  }

  return (
    <div className={cn("relative h-full w-full overflow-hidden rounded-lg border border-border", className)}>
      <div ref={containerRef} style={{ width: "100%", height: "100%" }} />
    </div>
  );
}
