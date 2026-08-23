import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getDeclarationPerteList,
  deleteDeclarationPerte,
} from "@/lib/api/declaration-perte";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, FileWarning, Pencil, Trash2 } from "lucide-react";
import type { DeclarationPerte } from "@/types";

export const DECLARATION_PERTE_MODULE = "sedentaire_secretariat_declaration_perte";

export function formatHeure(heure: string | null | undefined): string {
  return heure ? heure.slice(0, 5) : "—";
}

export function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

const LIST_PATH = "/sedentaire/secretariat/declaration-perte";

export function DeclarationPerteList() {
  const [declarations, setDeclarations] = useState<DeclarationPerte[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<DeclarationPerte | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, DECLARATION_PERTE_MODULE, "can_create");
  const canDelete = hasPermission(user, DECLARATION_PERTE_MODULE, "can_delete");

  useEffect(() => {
    loadDeclarations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadDeclarations() {
    setLoading(true);
    try {
      const data = await getDeclarationPerteList();
      setDeclarations(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les déclarations de perte");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteDeclarationPerte(id);
      addNotification("success", "Supprimée", "Déclaration de perte supprimée avec succès");
      loadDeclarations();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer cette déclaration de perte");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<DeclarationPerte>[] = [
    {
      key: "date_declaration",
      header: "Date",
      sortable: true,
      render: (d) => formatDate(d.date_declaration),
    },
    {
      key: "heure_declaration",
      header: "Heure",
      sortable: true,
      render: (d) => formatHeure(d.heure_declaration),
    },
    { key: "numero_attestation", header: "N° attestation", sortable: true },
    { key: "identite_declarant", header: "Déclarant", sortable: true },
    { key: "nature_objet", header: "Nature de l'objet", sortable: true },
    { key: "lieu_perte", header: "Lieu de perte", sortable: true },
    { key: "nom_agent", header: "Agent", sortable: true },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (d) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`${LIST_PATH}/${d.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(d)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Déclaration de perte</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Registre des déclarations de perte de documents et objets
          </p>
        </div>
        {canCreate && (
          <Button
            onClick={() => navigate(`${LIST_PATH}/new`)}
            className="gap-2"
          >
            <Plus className="h-4 w-4" />
            Nouvelle déclaration
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <CardTitle className="text-base flex items-center gap-2">
              <FileWarning className="h-4 w-4" />
              Déclarations de perte ({declarations.length})
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={declarations}
            keyExtractor={(d) => d.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par attestation, déclarant, objet..."
            emptyMessage="Aucune déclaration de perte"
            onRowClick={(d) => navigate(`${LIST_PATH}/${d.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la déclaration de perte"
        message={`Êtes-vous sûr de vouloir supprimer la déclaration de perte ${deleteTarget?.numero_attestation} (${deleteTarget?.identite_declarant}) ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
