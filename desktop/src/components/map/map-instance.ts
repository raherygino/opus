import type { Map as MapboxMap } from "mapbox-gl";

let _mapInstance: MapboxMap | null = null;

export function setMapInstance(map: MapboxMap | null) {
  _mapInstance = map;
}

export function getMapInstance(): MapboxMap | null {
  return _mapInstance;
}
