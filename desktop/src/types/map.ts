export interface MapViewport {
  center: [number, number];
  zoom: number;
  bounds?: [[number, number], [number, number]];
}

export interface MapMarker {
  id: string;
  position: [number, number];
  title?: string;
  description?: string;
  icon?: "default" | "incident" | "patrol" | "officer" | "station";
  color?: string;
  data?: Record<string, unknown>;
}

export interface MapRoute {
  id: string;
  name: string;
  coordinates: [number, number][];
  color?: string;
  weight?: number;
  opacity?: number;
  data?: Record<string, unknown>;
}

export interface MapPolygon {
  id: string;
  name: string;
  coordinates: [number, number][];
  color?: string;
  fillColor?: string;
  fillOpacity?: number;
  data?: Record<string, unknown>;
}

export type MapLayerType = "streets" | "satellite";

export type ActiveLayer = "streets" | "satellite";

export interface GeoJSONFeature {
  type: "Feature";
  geometry: {
    type: string;
    coordinates: unknown;
  };
  properties: Record<string, unknown>;
}

export interface GeoJSONCollection {
  type: "FeatureCollection";
  features: GeoJSONFeature[];
}
