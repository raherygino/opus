import { motion, AnimatePresence } from "framer-motion";
import { useMapStore } from "@/stores/map-store";
import {
  MapPin,
  Route,
  Layers,
  Globe,
  Crosshair,
  Download,
  Upload,
  PanelRightClose,
  PanelRight,
} from "lucide-react";

const toolSections = [
  {
    id: "markers",
    label: "Points d'intérêt",
    icon: MapPin,
    tools: [
      { id: "add-marker", label: "Ajouter un marqueur", icon: MapPin },
      { id: "import-poi", label: "Importer des POI", icon: Upload },
    ],
  },
  {
    id: "layers",
    label: "Couches",
    icon: Layers,
    tools: [
      { id: "streets", label: "Plan", icon: Globe },
      { id: "satellite", label: "Satellite", icon: Globe },
    ],
  },
  {
    id: "routes",
    label: "Itinéraires",
    icon: Route,
    tools: [
      { id: "draw-route", label: "Dessiner un itinéraire", icon: Route },
      { id: "geolocation", label: "Géolocalisation", icon: Crosshair },
    ],
  },
  {
    id: "data",
    label: "Données GIS",
    icon: Download,
    tools: [
      { id: "export", label: "Exporter", icon: Download },
      { id: "import", label: "Importer", icon: Upload },
    ],
  },
];

export function MapToolsPanel() {
  const showToolsPanel = useMapStore((s) => s.showToolsPanel);
  const toggleToolsPanel = useMapStore((s) => s.toggleToolsPanel);

  return (
    <AnimatePresence initial={false}>
      {showToolsPanel ? (
        <motion.div
          key="panel-open"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -20 }}
          transition={{ duration: 0.15, ease: "easeInOut" }}
          className="absolute left-2 top-2 z-[1000] w-60 rounded-lg border border-border bg-card shadow-lg"
        >
          <div className="flex items-center justify-between border-b border-border px-3 py-2">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Outils cartographie
            </h3>
            <button
              onClick={toggleToolsPanel}
              className="flex h-6 w-6 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
              title="Masquer les outils"
            >
              <PanelRightClose className="h-3.5 w-3.5" />
            </button>
          </div>
          <div className="divide-y divide-border">
            {toolSections.map((section) => (
              <div key={section.id} className="p-1">
                <div className="flex items-center gap-2 px-2 py-1.5">
                  <section.icon className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-[11px] font-medium text-muted-foreground">
                    {section.label}
                  </span>
                </div>
                {section.tools.map((tool) => (
                  <button
                    key={tool.id}
                    disabled
                    className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-xs text-muted-foreground/60 opacity-60 hover:bg-accent hover:text-accent-foreground"
                    title="Fonctionnalité à venir"
                  >
                    <tool.icon className="h-3.5 w-3.5" />
                    {tool.label}
                    <span className="ml-auto text-[9px] text-muted-foreground/40">API</span>
                  </button>
                ))}
              </div>
            ))}
          </div>
        </motion.div>
      ) : (
        <motion.button
          key="panel-closed"
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.9 }}
          transition={{ duration: 0.12, ease: "easeOut" }}
          onClick={toggleToolsPanel}
          className="absolute left-2 top-2 z-[1000] flex h-8 w-8 items-center justify-center rounded-md border border-border bg-card text-muted-foreground shadow-sm transition-colors hover:bg-accent hover:text-accent-foreground"
          title="Afficher les outils"
        >
          <PanelRight className="h-4 w-4" />
        </motion.button>
      )}
    </AnimatePresence>
  );
}
