export const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN as string | undefined;

export const MAPBOX_STYLES = {
  streets: {
    light: "mapbox://styles/mapbox/streets-v12",
    dark: "mapbox://styles/mapbox/navigation-night-v1",
  },
  satellite: {
    light: "mapbox://styles/mapbox/satellite-v9",
    dark: "mapbox://styles/mapbox/satellite-streets-v12",
  },
} as const;
