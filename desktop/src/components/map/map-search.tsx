import { useState, useEffect, useRef } from "react";
import { Search, Navigation, Loader2 } from "lucide-react";
import { useDebounce } from "@/hooks/use-debounce";
import { getMapInstance } from "./map-instance";
import { mapboxToken } from "@/config/map";

interface GeocodingResult {
  display_name: string;
  lat: string;
  lon: string;
  type?: string;
  importance?: number;
  place_name?: string;
  center?: [number, number];
}

function searchNominatim(query: string, signal: AbortSignal): Promise<GeocodingResult[]> {
  return fetch(
    `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=5&accept-language=fr`,
    { signal },
  )
    .then((res) => res.json())
    .then((data: GeocodingResult[]) => data);
}

function searchMapbox(query: string, signal: AbortSignal): Promise<GeocodingResult[]> {
  return fetch(
    `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${mapboxToken}&limit=5&language=fr&types=place,locality,neighborhood,address,poi`,
    { signal },
  )
    .then((res) => res.json())
    .then((data: { features: { place_name: string; center: [number, number]; id: string }[] }) =>
      data.features.map((f) => ({
        display_name: f.place_name,
        lat: String(f.center[1]),
        lon: String(f.center[0]),
      })),
    );
}

export function MapSearch() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<GeocodingResult[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const debouncedQuery = useDebounce(query, 400);

  useEffect(() => {
    if (!debouncedQuery || debouncedQuery.length < 2) {
      setResults([]);
      setIsOpen(false);
      return;
    }

    const controller = new AbortController();
    setIsLoading(true);
    const search = mapboxToken ? searchMapbox : searchNominatim;

    search(debouncedQuery, controller.signal)
      .then((data) => {
        setResults(data);
        setIsOpen(data.length > 0);
        setSelectedIndex(-1);
      })
      .catch(() => {})
      .finally(() => setIsLoading(false));

    return () => controller.abort();
  }, [debouncedQuery]);

  const handleSelect = (result: GeocodingResult) => {
    const map = getMapInstance();
    if (map) {
      map.flyTo({
        center: [parseFloat(result.lon), parseFloat(result.lat)],
        zoom: 16,
        duration: 1000,
      });
    }
    setQuery((result.display_name || "").split(",")[0]);
    setIsOpen(false);
    inputRef.current?.blur();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Escape") {
      setIsOpen(false);
      inputRef.current?.blur();
    }
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % results.length);
    }
    if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelectedIndex((prev) => (prev - 1 + results.length) % results.length);
    }
    if (e.key === "Enter" && selectedIndex >= 0 && results[selectedIndex]) {
      handleSelect(results[selectedIndex]);
    }
  };

  return (
    <div className="relative w-72">
      <div className="relative">
        <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
        <input
          ref={inputRef}
          type="text"
          placeholder="Rechercher un lieu..."
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setSelectedIndex(-1);
          }}
          onFocus={() => {
            if (results.length > 0) setIsOpen(true);
          }}
          onKeyDown={handleKeyDown}
          className="h-8 w-full rounded-md border border-border bg-card pl-8 pr-8 text-xs text-foreground placeholder:text-muted-foreground shadow-sm outline-none transition-colors focus:border-ring focus:ring-1 focus:ring-ring"
        />
        {isLoading && (
          <Loader2 className="absolute right-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 animate-spin text-muted-foreground" />
        )}
      </div>

      {isOpen && results.length > 0 && (
        <div className="absolute top-full mt-1 w-full rounded-md border border-border bg-card shadow-lg">
          {results.map((result, index) => (
            <button
              key={result.lat + result.lon + result.display_name}
              onClick={() => handleSelect(result)}
              onMouseEnter={() => setSelectedIndex(index)}
              className={[
                "flex w-full items-start gap-2 px-3 py-2 text-left text-xs transition-colors",
                index === selectedIndex
                  ? "bg-accent text-accent-foreground"
                  : "text-foreground hover:bg-accent/50",
                index === 0 ? "rounded-t-md" : "",
                index === results.length - 1 ? "rounded-b-md" : "",
              ].join(" ")}
            >
              <Navigation className="mt-0.5 h-3 w-3 shrink-0 text-muted-foreground" />
              <div className="min-w-0 flex-1">
                <div className="truncate font-medium">
                  {(result.display_name || result.place_name || "").split(",")[0]}
                </div>
                <div className="mt-0.5 truncate text-[10px] text-muted-foreground">
                  {result.display_name || result.place_name || ""}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
