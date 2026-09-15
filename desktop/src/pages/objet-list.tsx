import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getObjetSaisiList,
  deleteObjetSaisi,
  getObjetTrouveList,
  deleteObjetTrouve,
} from "@/lib/api/objet";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { Plus, Pencil, Trash2, Package, CheckCircle2 } from "lucide-react";
import type { ObjetSaisi, ObjetTrouve } from "@/types";
import { OBJET_SAISI_TYPE_LABELS, OBJET_TROUVE_MOTIF_LABELS } from "@/types";

const PJ_MODULE = "pj_objets";

export function ObjetList() {
  const [saisiItems, setSaisiItems] = useState<ObjetSaisi[]>([]);
  const [trouveItems, setTrouveItems] = useState<ObjetTrouve[]>([]);
  const [tab, setTab] = useState("saisi");
  const [loading, setLoading] = useState(true);
  const [deleteSaisiTarget, setDeleteSaisiTarget] = useState<ObjetSaisi | null>(null);
  const [deleteTrouveTarget, setDeleteTrouveTarget] = useState<ObjetTrouve | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const [saisi, trouve] = await Promise.all([
        getObjetSaisiList().catch(() => []),
        getObjetTrouveList().catch(() => []),
      ]);
      setSaisiItems(saisi);
      setTrouveItems(trouve);
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteSaisiTarget) {
        await deleteObjetSaisi(deleteSaisiTarget.id);
        addNotification("success", "Supprimé", "Objet saisi supprimé avec succès");
      } else if (deleteTrouveTarget) {
        await deleteObjetTrouve(deleteTrouveTarget.id);
        addNotification("success", "Supprimé", "Objet trouvé supprimé avec succès");
      }
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteSaisiTarget(null);
      setDeleteTrouveTarget(null);
    }
  }

  const saisiColumns: Column<ObjetSaisi>[] = [
    {
      key: "numero_dossier",
      header: "N° dossier",
      sortable: true,
      render: (r) => <span className="font-mono text-sm">{r.numero_dossier ?? "—"}</span>,
    },
    {
      key: "type_objet",
      header: "Type",
      sortable: true,
      render: (r) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary">
          {OBJET_SAISI_TYPE_LABELS[r.type_objet] ?? r.type_objet}
        </span>
      ),
    },
    {
      key: "motif",
      header: "Motif",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.motif}</span>,
    },
    {
      key: "proprietaire",
      header: "Propriétaire",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.proprietaire ?? "—"}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (r) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/objets/saisi/${r.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteSaisiTarget(r)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  const trouveColumns: Column<ObjetTrouve>[] = [
    {
      key: "affaire",
      header: "Affaire",
      sortable: true,
      render: (r) => <span className="font-medium line-clamp-1 max-w-xs">{r.affaire}</span>,
    },
    {
      key: "motif_decouverte",
      header: "Motif de découverte",
      sortable: true,
      render: (r) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground">
          {OBJET_TROUVE_MOTIF_LABELS[r.motif_decouverte] ?? r.motif_decouverte}
        </span>
      ),
    },
    {
      key: "restitution",
      header: "Restitution",
      render: (r) =>
        r.restitution === 1 ? (
          <span className="inline-flex items-center gap-1 text-xs text-primary">
            <CheckCircle2 className="h-3.5 w-3.5" />
            Restitué
          </span>
        ) : (
          <span className="text-xs text-muted-foreground">Non restitué</span>
        ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (r) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/objets/trouve/${r.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTrouveTarget(r)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-primary/10">
            <Package className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Objets</h1>
            <p className="text-sm text-muted-foreground">
              Objets saisis et trouvés (Police Judiciaire)
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate(`/pj/objets/${tab}/new`)}>
            <Plus className="h-4 w-4 mr-2" />
            {tab === "saisi" ? "Nouvel objet saisi" : "Nouvel objet trouvé"}
          </Button>
        )}
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="saisi">Objet saisi</TabsTrigger>
          <TabsTrigger value="trouve">Objet trouvé</TabsTrigger>
        </TabsList>

        <TabsContent value="saisi">
          <Card>
            <CardHeader>
              <CardTitle>Objets saisis</CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                data={saisiItems}
                columns={saisiColumns}
                loading={loading}
                keyExtractor={(r) => r.id}
                onRowClick={(r) => navigate(`/pj/objets/saisi/${r.id}`)}
                emptyMessage="Aucun objet saisi à afficher"
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="trouve">
          <Card>
            <CardHeader>
              <CardTitle>Objets trouvés</CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                data={trouveItems}
                columns={trouveColumns}
                loading={loading}
                keyExtractor={(r) => r.id}
                onRowClick={(r) => navigate(`/pj/objets/trouve/${r.id}`)}
                emptyMessage="Aucun objet trouvé à afficher"
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <ConfirmDialog
        open={deleteSaisiTarget !== null || deleteTrouveTarget !== null}
        title="Supprimer l'objet"
        message="Voulez-vous vraiment supprimer cet objet ? Toutes les pièces jointes seront également supprimées."
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        variant="destructive"
        onConfirm={handleDelete}
        onCancel={() => { setDeleteSaisiTarget(null); setDeleteTrouveTarget(null); }}
        loading={deleting}
      />
    </div>
  );
}
