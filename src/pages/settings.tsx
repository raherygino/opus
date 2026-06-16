import { useState } from "react";
import { useThemeStore } from "@/stores/theme-store";
import { motion } from "framer-motion";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import {
  Monitor,
  Moon,
  Sun,
  Type,
  Keyboard,
  Palette,
  Bell,
  Database,
  Users,
  Shield,
} from "lucide-react";

const sections = [
  { id: "appearance", label: "Appearance", icon: Palette },
  { id: "editor", label: "Editor", icon: Type },
  { id: "shortcuts", label: "Keyboard Shortcuts", icon: Keyboard },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "storage", label: "Storage", icon: Database },
  { id: "account", label: "Account", icon: Users },
  { id: "privacy", label: "Privacy & Security", icon: Shield },
];

const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.06 },
  },
};

const sectionItem = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0 },
};

export function Settings() {
  const { theme, setTheme } = useThemeStore();
  const [activeSection, setActiveSection] = useState("appearance");
  const [fontSize, setFontSize] = useState("14");

  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="show"
      className="mx-auto flex max-w-5xl gap-8 h-full"
    >
      <motion.aside variants={sectionItem} className="w-48 shrink-0 space-y-1">
        {sections.map((section) => {
          const Icon = section.icon;
          return (
            <button
              key={section.id}
              onClick={() => setActiveSection(section.id)}
              className={cn(
                "flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-xs font-medium transition-colors",
                activeSection === section.id
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground hover:text-foreground hover:bg-accent/50",
              )}
            >
              <Icon className="h-4 w-4" />
              {section.label}
            </button>
          );
        })}
      </motion.aside>

      <motion.div variants={sectionItem} className="flex-1 space-y-6 pb-12">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Manage your workspace preferences.
          </p>
        </div>

        {activeSection === "appearance" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <Card>
              <CardHeader>
                <CardTitle>Theme</CardTitle>
                <CardDescription>
                  Choose your preferred color scheme.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex gap-3">
                  {[
                    { value: "dark", label: "Dark", icon: Moon, desc: "Easy on the eyes" },
                    { value: "light", label: "Light", icon: Sun, desc: "Classic brightness" },
                  ].map((option) => {
                    const Icon = option.icon;
                    return (
                      <button
                        key={option.value}
                        onClick={() => setTheme(option.value as "dark" | "light")}
                        className={cn(
                          "flex flex-1 flex-col items-center gap-3 rounded-xl border-2 p-4 transition-all",
                          theme === option.value
                            ? "border-primary bg-primary/5"
                            : "border-border hover:border-muted-foreground/30",
                        )}
                      >
                        <Icon className="h-6 w-6" />
                        <div className="text-center">
                          <div className="text-sm font-medium">{option.label}</div>
                          <div className="text-xs text-muted-foreground">
                            {option.desc}
                          </div>
                        </div>
                        {theme === option.value && (
                          <Badge variant="default" className="text-[10px] px-1.5">
                            Active
                          </Badge>
                        )}
                      </button>
                    );
                  })}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Sidebar</CardTitle>
                <CardDescription>
                  Configure sidebar behavior and appearance.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Default width</div>
                    <div className="text-xs text-muted-foreground">
                      Set the default sidebar width in pixels
                    </div>
                  </div>
                  <Input
                    type="number"
                    value={260}
                    className="w-20 h-8 text-center text-sm"
                    readOnly
                  />
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {activeSection === "editor" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card>
              <CardHeader>
                <CardTitle>Editor Preferences</CardTitle>
                <CardDescription>
                  Customize the note editing experience.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Font size</div>
                    <div className="text-xs text-muted-foreground">
                      Adjust the text size in the editor
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => setFontSize((s) => String(Math.max(12, Number(s) - 1)))}
                    >
                      -
                    </Button>
                    <Input
                      type="number"
                      value={fontSize}
                      onChange={(e) => setFontSize(e.target.value)}
                      className="w-16 h-8 text-center text-sm"
                    />
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => setFontSize((s) => String(Math.min(24, Number(s) + 1)))}
                    >
                      +
                    </Button>
                  </div>
                </div>
                <Separator />
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Font family</div>
                    <div className="text-xs text-muted-foreground">
                      Choose your preferred font
                    </div>
                  </div>
                  <Badge variant="secondary">Inter (System)</Badge>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {activeSection === "shortcuts" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card>
              <CardHeader>
                <CardTitle>Keyboard Shortcuts</CardTitle>
                <CardDescription>
                  Available keyboard shortcuts for quick actions.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="divide-y divide-border">
                  {[
                    { keys: "⌘P / Ctrl+P", action: "Command palette" },
                    { keys: "⌘B / Ctrl+B", action: "Toggle sidebar" },
                    { keys: "⌘⇧L / Ctrl+Shift+L", action: "Toggle theme" },
                    { keys: "G D", action: "Go to Dashboard" },
                    { keys: "G N", action: "Go to Notes" },
                    { keys: "G S", action: "Open Settings" },
                    { keys: "ESC", action: "Close dialogs / cancel" },
                    { keys: "↑↓", action: "Navigate command list" },
                  ].map((shortcut) => (
                    <div
                      key={shortcut.keys}
                      className="flex items-center justify-between py-2.5"
                    >
                      <span className="text-sm">{shortcut.action}</span>
                      <Badge variant="outline" className="font-mono text-[10px]">
                        {shortcut.keys}
                      </Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {![ "appearance", "editor", "shortcuts" ].includes(activeSection) && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex flex-col items-center justify-center py-16 text-center"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-muted">
              <Monitor className="h-6 w-6 text-muted-foreground" />
            </div>
            <h3 className="mt-4 text-sm font-medium">Coming soon</h3>
            <p className="mt-1 text-xs text-muted-foreground max-w-sm">
              This section is under development and will be available in a future update.
            </p>
          </motion.div>
        )}
      </motion.div>
    </motion.div>
  );
}
