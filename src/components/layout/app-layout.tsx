import { Outlet } from "react-router-dom";
import { useKeyboardShortcuts } from "@/hooks/use-keyboard";
import { useSidebarStore } from "@/stores/sidebar-store";
import { Sidebar } from "./sidebar";
import { TopBar } from "./top-bar";
import { StatusBar } from "./status-bar";
export function AppLayout() {
  useKeyboardShortcuts();
  const { isOpen, width } = useSidebarStore();

  return (
    <div className="flex h-screen flex-col">
      <TopBar />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main
          className="flex-1 overflow-auto"
          style={{ marginLeft: isOpen ? 0 : -width }}
        >
          <div className="h-full p-6">
            <Outlet />
          </div>
        </main>
      </div>
      <StatusBar />
    </div>
  );
}
