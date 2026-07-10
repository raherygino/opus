import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { useWindowStore } from "@/stores/window-store";
import { login as apiLogin } from "@/lib/api/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Eye, EyeOff, Loader2, Shield } from "lucide-react";
import { WindowControls } from "@/components/title-bar/window-controls";
import { TrafficLights } from "@/components/title-bar/traffic-lights";
import { cn } from "@/lib/utils";
import logoSrc from "@/assets/img/logo-opus.png";
import logoPnSrc from "@/assets/img/logo-pn.png";
import logoCspSrc from "@/assets/img/logo-csp.png";
import coverSrc from "@/assets/img/cover.jpg";

export function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const { focused, platform, setMaximized, setFocused, setPlatform } =
    useWindowStore();

  const isMac = platform === "darwin";

  useEffect(() => {
    setPlatform(
      (navigator.platform?.toLowerCase().includes("mac")
        ? "darwin"
        : navigator.platform?.toLowerCase().includes("win")
          ? "win32"
          : "linux") as "darwin" | "win32" | "linux",
    );
  }, [setPlatform]);

  useEffect(() => {
    if (!window.electronAPI) return;
    window.electronAPI.isMaximized().then(setMaximized);
    window.electronAPI.isFocused().then(setFocused);

    const unsubState = window.electronAPI.onWindowStateChanged(setMaximized);
    const unsubFocus = window.electronAPI.onFocusChanged(setFocused);

    return () => {
      unsubState();
      unsubFocus();
    };
  }, [setMaximized, setFocused]);

  const handleMinimize = () => window.electronAPI?.minimizeWindow();
  const handleMaximize = () => window.electronAPI?.maximizeWindow();
  const handleClose = () => window.electronAPI?.closeWindow();

  const from = (location.state as { from?: { pathname: string } })?.from
    ?.pathname || "/dashboard";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");

    if (!username.trim() || !password.trim()) {
      setError("Veuillez entrer votre nom d'utilisateur et mot de passe");
      return;
    }

    setLoading(true);
    try {
      const result = await apiLogin(username, password);
      login(result.user, result.access_token, result.refresh_token);
      addNotification("success", "Connexion réussie", `Bienvenue ${result.user.firstname} ${result.user.lastname}`);

      const role = result.user.role_code;
      if (role === "SUPER_ADMIN" || role === "CHIEF" || role === "STATION_ADMIN") {
        navigate("/dashboard");
      } else if (role === "HEAD_SG" || role === "OFFICER") {
        navigate("/sg/dashboard");
      } else if (role === "HEAD_SED" || role === "RECEPTION" || role === "CLERK") {
        navigate("/sedentaire/dashboard");
      } else if (role === "HEAD_PJ" || role === "INVESTIGATOR" || role === "CUSTODY") {
        navigate("/pj/dashboard");
      } else {
        navigate(from);
      }
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response: { data: { message: string } } }).response?.data
              ?.message || "Erreur de connexion"
          : "Erreur de connexion au serveur";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex h-screen w-screen flex-col bg-background">
      <header
        className={cn(
          "relative flex h-10 items-center border-b select-none draggable transition-colors duration-200 shrink-0 z-20",
          focused
            ? "border-border bg-background"
            : "border-border/50 bg-muted/30",
        )}
      >
        {isMac && (
          <TrafficLights
            onMinimize={handleMinimize}
            onMaximize={handleMaximize}
            onClose={handleClose}
          />
        )}

        <div className="flex flex-1 items-center justify-center px-4">
          <span className="text-sm font-semibold tracking-tight select-none">
            OPUS
          </span>
        </div>

        {!isMac && (
          <WindowControls
            onMinimize={handleMinimize}
            onMaximize={handleMaximize}
            onClose={handleClose}
          />
        )}
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Left Panel — Hero Section */}
        <div className="relative flex w-[60%] flex-col justify-between overflow-hidden">
          <div className="absolute inset-0 overflow-hidden pointer-events-none">
            {/* Cover image as full background */}
            <div className="absolute inset-0">
              <img src={coverSrc} alt="" className="h-full w-full object-cover" />
            </div>

            {/* Low-opacity overlay for readability */}
            <div className="absolute inset-0 bg-background/90" />

            <div
              className="absolute inset-0 opacity-[0.03]"
              style={{
                backgroundImage: `radial-gradient(circle at 1px 1px, hsl(var(--primary)) 1px, transparent 0)`,
                backgroundSize: "40px 40px",
              }}
            />
            {/* Floating rings & diamond grid */}
            <svg
              className="absolute top-1/4 right-8 w-[28rem] h-[28rem] text-primary/[0.04]"
              viewBox="0 0 400 400"
              fill="none"
            >
              <circle cx="200" cy="200" r="180" stroke="currentColor" strokeWidth="1" />
              <circle cx="200" cy="200" r="140" stroke="currentColor" strokeWidth="0.5" />
              <circle cx="200" cy="200" r="100" stroke="currentColor" strokeWidth="0.5" strokeDasharray="6 6" />
              <path d="M200 20 L330 200 L200 380 L70 200 Z" stroke="currentColor" strokeWidth="0.5" />
              <path d="M200 60 L300 200 L200 340 L100 200 Z" stroke="currentColor" strokeWidth="0.5" strokeDasharray="4 4" />
            </svg>

            {/* Scattered dots */}
            <svg
              className="absolute top-[15%] left-[10%] w-48 h-48 text-primary/[0.05]"
              viewBox="0 0 200 200"
              fill="currentColor"
            >
              <circle cx="20" cy="20" r="2" />
              <circle cx="80" cy="40" r="1.5" />
              <circle cx="160" cy="10" r="1" />
              <circle cx="40" cy="100" r="1.5" />
              <circle cx="120" cy="80" r="2.5" />
              <circle cx="180" cy="120" r="1" />
              <circle cx="10" cy="160" r="1.5" />
              <circle cx="90" cy="150" r="2" />
              <circle cx="150" cy="180" r="1.5" />
            </svg>

            {/* Wave curves at bottom */}
            <svg
              className="absolute bottom-0 left-0 w-full h-48 text-primary/[0.06]"
              viewBox="0 0 600 200"
              preserveAspectRatio="none"
              fill="none"
            >
              <path d="M0 120 C 150 180, 300 60, 600 120 L 600 200 L 0 200 Z" fill="currentColor" />
              <path d="M0 160 C 200 200, 400 80, 600 160 L 600 200 L 0 200 Z" fill="currentColor" opacity="0.5" />
            </svg>

          </div>

          <div className="relative z-10 flex flex-col justify-between h-full p-8 lg:p-12">
            <div>
              <img src={logoCspSrc} alt="PN" className="h-32 w-auto object-contain" />
            </div>

            <div className="flex-1 flex flex-col justify-center -mt-16">
              <motion.h1
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.1 }}
                className="text-5xl font-bold tracking-tight text-foreground"
              >
                Bienvenue
              </motion.h1>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.5, delay: 0.15 }}
                className="h-px w-24 bg-primary/30 my-4"
              />

              <motion.h2
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.2 }}
                className="mt-2 text-5xl font-bold tracking-tight text-primary"
              >
                OPUS
              </motion.h2>
              <motion.p
                initial={{ opacity: 0, y: 24 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.3 }}
                className="mt-4 text-lg text-muted-foreground/80 leading-relaxed"
              >
                Opérations Policières Unifiées et Structurées — la plateforme intégrée de gestion des opérations policières.
              </motion.p>
            </div>

            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground/60">
                &copy; {new Date().getFullYear()} OPUS — Tous droits réservés
              </p>
              <img src={logoPnSrc} alt="CSP" className="h-32 w-auto object-contain opacity-60" />
            </div>
          </div>
        </div>

        {/* Right Panel — Login Form */}
        <div className="flex w-[40%] flex-col items-center justify-center p-8 lg:p-12 overflow-y-auto">
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="w-full max-w-sm"
          >
            <div className="mb-8 text-center">
              <img src={logoSrc} alt="OPUS" className="mx-auto h-64 w-auto object-contain" />
            </div>

            <h1 className="text-xl tracking-tight text-foreground text-center">
              Connectez-vous pour accéder au système
            </h1>

            <form onSubmit={handleSubmit} className="mt-8 space-y-5">
              <div className="space-y-2">
                <Label htmlFor="username">Nom d'utilisateur</Label>
                <Input
                  id="username"
                  placeholder="Entrez votre nom d'utilisateur"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoFocus
                  disabled={loading}
                  className="h-11 rounded-lg"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">Mot de passe</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    placeholder="Entrez votre mot de passe"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    disabled={loading}
                    className="h-11 rounded-lg pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                    tabIndex={-1}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>

              {error && (
                <p className="text-sm text-destructive bg-destructive/10 rounded-lg px-3 py-2">
                  {error}
                </p>
              )}

              <Button
                type="submit"
                className="w-full h-11 rounded-lg text-base font-medium gap-2"
                disabled={loading}
              >
                {loading ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <Shield className="h-5 w-5" />
                )}
                {loading ? "Connexion en cours..." : "Se connecter"}
              </Button>

              <div className="text-center">
                <button
                  type="button"
                  className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                >
                  Mot de passe oublié ?
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
