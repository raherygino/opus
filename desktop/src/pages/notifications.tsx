import { useState, useEffect, useCallback, useMemo } from "react";
import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { useNotificationStore } from "@/stores/notification-store";
import { useAuthStore } from "@/stores/auth-store";
import {
  getNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  deleteNotification,
} from "@/lib/api/notifications";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Bell,
  BellRing,
  CheckCheck,
  Trash2,
  Info,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Scale,
  Car,
  Building2,
  Clock,
} from "lucide-react";
import type { AppNotification } from "@/types";

const SERVICE_LABELS: Record<string, { label: string; icon: React.ElementType; color: string }> = {
  PJ: { label: "Police Judiciaire", icon: Scale, color: "text-blue-500" },
  SG: { label: "Service Général", icon: Car, color: "text-green-500" },
  Sedentaire: { label: "Sédentaire", icon: Building2, color: "text-purple-500" },
  System: { label: "Système", icon: Info, color: "text-muted-foreground" },
};

const TYPE_ICONS: Record<string, React.ElementType> = {
  info: Info,
  warning: AlertTriangle,
  success: CheckCircle,
  error: XCircle,
};

const TYPE_COLORS: Record<string, string> = {
  info: "text-blue-500",
  warning: "text-amber-500",
  success: "text-green-500",
  error: "text-red-500",
};

function formatTimeAgo(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diff < 60) return "à l'instant";
  if (diff < 3600) return `il y a ${Math.floor(diff / 60)} min`;
  if (diff < 86400) return `il y a ${Math.floor(diff / 3600)} h`;
  if (diff < 604800) return `il y a ${Math.floor(diff / 86400)} j`;
  return date.toLocaleDateString("fr-FR");
}

export function NotificationsPage() {
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const { user } = useAuthStore();
  const isAdmin = user?.role_code === "SUPER_ADMIN" || user?.role_code === "STATION_ADMIN";
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("all");
  const [deleteTarget, setDeleteTarget] = useState<AppNotification | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getNotifications();
      setNotifications(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les notifications");
    } finally {
      setLoading(false);
    }
  }, [addNotification]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const filtered = notifications.filter((n) => {
    if (activeTab === "all") return true;
    if (activeTab === "unread") return n.is_read === 0;
    return n.service === activeTab;
  });

  const unreadCount = notifications.filter((n) => n.is_read === 0).length;

  async function handleMarkAsRead(id: number) {
    try {
      await markNotificationAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, is_read: 1 } : n)),
      );
    } catch {
      addNotification("error", "Erreur", "Impossible de marquer comme lu");
    }
  }

  async function handleMarkAllAsRead() {
    setMarkingAll(true);
    try {
      await markAllNotificationsAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, is_read: 1 })));
      addNotification("success", "Lu", "Toutes les notifications marquées comme lues");
    } catch {
      addNotification("error", "Erreur", "Impossible de tout marquer comme lu");
    } finally {
      setMarkingAll(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      addNotification("success", "Supprimé", "Notification supprimée");
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer la notification");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  function renderNotificationCard(n: AppNotification) {
    const serviceInfo = SERVICE_LABELS[n.service] || SERVICE_LABELS.System;
    const TypeIcon = TYPE_ICONS[n.type] || Info;
    const ServiceIcon = serviceInfo.icon;

    return (
      <Card
        key={n.id}
        className={`relative transition-all ${n.is_read === 0 ? "border-primary/30 bg-primary/5" : "opacity-75"}`}
      >
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <div className={`mt-0.5 shrink-0 ${TYPE_COLORS[n.type] || "text-muted-foreground"}`}>
              <TypeIcon className="h-5 w-5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <p className="text-sm font-semibold">{n.title}</p>
                {n.is_read === 0 && (
                  <span className="h-2 w-2 rounded-full bg-primary shrink-0" />
                )}
              </div>
              {n.message && (
                <p className="text-sm text-muted-foreground mt-1">{n.message}</p>
              )}
              <div className="flex items-center gap-3 mt-2 flex-wrap">
                <span className={`inline-flex items-center gap-1 text-xs ${serviceInfo.color}`}>
                  <ServiceIcon className="h-3 w-3" />
                  {serviceInfo.label}
                </span>
                <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                  <Clock className="h-3 w-3" />
                  {formatTimeAgo(n.created_at)}
                </span>
                {n.personnel_lastname && (
                  <button
                    className="text-xs text-primary hover:underline"
                    onClick={() => navigate(`/personnel/${n.personnel_id}`)}
                  >
                    {n.personnel_firstname} {n.personnel_lastname} ({n.personnel_im})
                  </button>
                )}
                {n.created_by_username && (
                  <span className="text-xs text-muted-foreground">
                    par {n.created_by_username}
                  </span>
                )}
              </div>
            </div>
            <div className="flex items-center gap-1 shrink-0">
              {n.is_read === 0 && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => handleMarkAsRead(n.id)}
                  title="Marquer comme lu"
                >
                  <CheckCheck className="h-3.5 w-3.5" />
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7 text-destructive"
                onClick={() => setDeleteTarget(n)}
                title="Supprimer"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  const tabs = useMemo(() => {
    const base = [
      { value: "all", label: "Toutes", count: notifications.length },
      { value: "unread", label: "Non lues", count: unreadCount },
    ];
    if (isAdmin) {
      base.push(
        { value: "PJ", label: "PJ", count: notifications.filter((n) => n.service === "PJ" && n.is_read === 0).length },
        { value: "SG", label: "Service Général", count: notifications.filter((n) => n.service === "SG" && n.is_read === 0).length },
        { value: "Sedentaire", label: "Sédentaire", count: notifications.filter((n) => n.service === "Sedentaire" && n.is_read === 0).length },
      );
    }
    return base;
  }, [notifications, unreadCount, isAdmin]);

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight flex items-center gap-2">
            <BellRing className="h-6 w-6" />
            Notifications
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isAdmin
              ? "Toutes les notifications — PJ, Service Général, Sédentaire"
              : "Vos notifications"}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button
            variant="secondary"
            onClick={handleMarkAllAsRead}
            disabled={markingAll}
            className="gap-2"
          >
            <CheckCheck className="h-4 w-4" />
            Tout marquer comme lu
          </Button>
        )}
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="flex-wrap">
          {tabs.map((t) => (
            <TabsTrigger key={t.value} value={t.value} className="gap-1.5">
              {t.label}
              {t.count > 0 && (
                <span className={`text-xs rounded-full px-1.5 ${
                  t.value === "all" ? "bg-muted text-muted-foreground" : "bg-primary text-primary-foreground"
                }`}>
                  {t.count}
                </span>
              )}
            </TabsTrigger>
          ))}
        </TabsList>

        {tabs.map((t) => (
          <TabsContent key={t.value} value={t.value} className="space-y-3 mt-4">
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
              </div>
            ) : filtered.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center text-muted-foreground">
                  <Bell className="h-10 w-10 mx-auto mb-3 opacity-30" />
                  <p>Aucune notification</p>
                </CardContent>
              </Card>
            ) : (
              filtered.map(renderNotificationCard)
            )}
          </TabsContent>
        ))}
      </Tabs>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la notification"
        message="Êtes-vous sûr de vouloir supprimer cette notification ?"
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
