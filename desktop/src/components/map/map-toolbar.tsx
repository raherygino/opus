import { useMapStore } from "@/stores/map-store";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Maximize2,
  Minimize2,
  ZoomIn,
  ZoomOut,
  Globe,
  Satellite,
} from "lucide-react";
import { getMapInstance } from "./map-instance";

const layers = [
  { id: "streets" as const, label: "Plan", icon: Globe },
  { id: "satellite" as const, label: "Satellite", icon: Satellite },
];

export function MapToolbar() {
  const activeLayer = useMapStore((s) => s.activeLayer);
  const setActiveLayer = useMapStore((s) => s.setActiveLayer);
  const isFullscreen = useMapStore((s) => s.isFullscreen);
  const setFullscreen = useMapStore((s) => s.setFullscreen);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setFullscreen(false);
    }
  };

  return (
    <div className="flex items-center gap-1.5">
      <div className="flex items-center rounded-md border border-border bg-card p-0.5 shadow-sm">
        {layers.map((layer) => {
          const Icon = layer.icon;
          return (
            <Tooltip key={layer.id}>
              <TooltipTrigger asChild>
                <button
                  onClick={() => setActiveLayer(layer.id)}
                  className={cn(
                    "flex items-center gap-1.5 rounded px-2.5 py-1 text-xs font-medium transition-colors",
                    activeLayer === layer.id
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  <Icon className="h-3.5 w-3.5" />
                  {layer.label}
                </button>
              </TooltipTrigger>
              <TooltipContent side="bottom">
                Passer à {layer.label.toLowerCase()}
              </TooltipContent>
            </Tooltip>
          );
        })}
      </div>

      <div className="flex items-center gap-1">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              className="h-7 w-7"
              onClick={() => getMapInstance()?.zoomIn()}
            >
              <ZoomIn className="h-3.5 w-3.5" />
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">Zoom avant</TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              className="h-7 w-7"
              onClick={() => getMapInstance()?.zoomOut()}
            >
              <ZoomOut className="h-3.5 w-3.5" />
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">Zoom arrière</TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              className="h-7 w-7"
              onClick={toggleFullscreen}
            >
              {isFullscreen ? (
                <Minimize2 className="h-3.5 w-3.5" />
              ) : (
                <Maximize2 className="h-3.5 w-3.5" />
              )}
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">
            {isFullscreen ? "Quitter plein écran" : "Plein écran"}
          </TooltipContent>
        </Tooltip>
      </div>
    </div>
  );
}
