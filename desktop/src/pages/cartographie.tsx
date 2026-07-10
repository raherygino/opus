import { motion } from "framer-motion";
import { MapView, MapToolsPanel, MapToolbar, MapSearch } from "@/components/map";

export function Cartographie() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="flex h-full flex-col space-y-3"
    >
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Cartographie</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Visualisation et gestion des données géospatiales
          </p>
        </div>
        <MapToolbar />
      </div>

      <div className="relative flex-1">
        <MapToolsPanel />
        <div className="absolute top-2 left-1/2 z-[1000] -translate-x-1/2">
          <MapSearch />
        </div>
        <MapView className="h-full w-full" />
      </div>
    </motion.div>
  );
}
