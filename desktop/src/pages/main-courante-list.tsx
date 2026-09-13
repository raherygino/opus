import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getMainCouranteList,
  deleteMainCourante,
  getMainCouranteCategories,
} from "@/lib/api/main-courante";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, BookOpenText, Pencil, Trash2 } from "lucide-react";
import type { MainCourante, MainCouranteCategorieItem, MainCouranteOrigine } from "@/types";

// Default color for categories without an explicit mapping. Existing
// entries keep their historical colors; user-added categories fall back to
// the muted style.
const CATEGORIE_COLORS: Record<string, string> = {
  "Entrée/Sortie de tiers": "bg-blue-500/10 text-blue-500",
  "Incident au poste": "bg-amber-500/10 text-amber-500",
  "Renseignement reçu": "bg-green-500/10 text-green-500",
};

/** Resolve the origine (Secretariat / Poste) from the current URL path. */
function useOrigine(): MainCouranteOrigine {
  const location = useLocation();
  return location.pathname.includes("/poste/") ? "Poste" : "Secretariat";
}

/** The permission module code for the current origine context. */
function moduleForOrigine(origine: MainCouranteOrigine): string {
  return origine === "Poste"
    ? "sedentaire_poste_main_courante"
    : "sedentaire_secretariat_main_courante";
}

/** The base list path for the current origine context. */
export function listPathForOrigine(origine: MainCouranteOrigine): string {
  return origine === "Poste"
    ? "/sedentaire/poste/main-courante"
    : "/sedentaire/secretariat/main-courante";
}

export function formatHeure(heure: string | null | undefined): string {
  return heure ? heure.slice(0, 5) : "—";
}

export function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

export function MainCouranteList() {
  const origine = useOrigine();
  const module = moduleForOrigine(origine);
  const listPath = listPathForOrigine(origine);

  const [entries, setEntries] = useState<MainCourante[]>([]);
  const [loading, setLoading] = useState(true);
  const [categorieFilter, setCategorieFilter] = useState("");
  const [categories, setCategories] = useState<MainCouranteCategorieItem[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<MainCourante | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, module, "can_create");
  const canDelete = hasPermission(user, module, "can_delete");

  useEffect(() => {
    loadEntries();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categorieFilter, origine]);

  useEffect(() => {
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadCategories() {
    try {
      const data = await getMainCouranteCategories();
      setCategories(data);
    } catch {
      // Non-blocking — the filter just won't show custom categories.
    }
  }

  async function loadEntries() {
    setLoading(true);
    try {
      const filters: Record<string, string> = { origine };
      if (categorieFilter) filters.categorie = categorieFilter;
      const data = await getMainCouranteList(filters);
      setEntries(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger la main courante");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteMainCourante(id);
      addNotification("success", "Supprimée", "Main courante supprimée avec succès");
      loadEntries();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer cette main courante");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<MainCourante>[] = [
    {
      key: "categorie",
      header: "Catégorie",
      sortable: true,
      render: (e) => (
        <span className={`text-xs px-2 py-0.5 rounded-full ${CATEGORIE_COLORS[e.categorie] ?? "bg-muted text-muted-foreground"}`}>
          {e.categorie}
        </span>
      ),
    },
    {
      key: "date_evenement",
      header: "Période",
      sortable: true,
      render: (e) => `${formatDate(e.date_evenement)} ${formatHeure(e.heure_evenement)}`,
    },
    {
      key: "description",
      header: "Description des faits",
      sortable: true,
      render: (e) => (
        <span className="line-clamp-2 max-w-md inline-block">{e.description}</span>
      ),
    },
    {
      key: "agent",
      header: "Agent",
      render: (e) =>
        [e.agent_prenoms, e.agent_nom].filter(Boolean).join(" ") ||
        e.agent_username ||
        "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (e) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`${listPath}/${e.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(e)}
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
          <h1 className="text-2xl font-semibold tracking-tight">Main courante</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Registre des événements — {origine === "Poste" ? "Poste" : "Secrétariat"}
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate(`${listPath}/new`)} className="gap-2">
            <Plus className="h-4 w-4" />
            Nouvelle entrée
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <CardTitle className="text-base flex items-center gap-2">
              <BookOpenText className="h-4 w-4" />
              Entrées ({entries.length})
            </CardTitle>
            <div className="flex items-center gap-2">
              <Select
                value={categorieFilter}
                onChange={(e) => setCategorieFilter(e.target.value)}
                options={[
                  { value: "", label: "Toutes les catégories" },
                  ...categories.map((c) => ({ value: c.label, label: c.label })),
                ]}
                className="w-56"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={entries}
            keyExtractor={(e) => e.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par description, catégorie..."
            emptyMessage="Aucune entrée de main courante"
            onRowClick={(e) => navigate(`${listPath}/${e.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'entrée"
        message={`Êtes-vous sûr de vouloir supprimer cette entrée de la main courante (${deleteTarget?.categorie}) ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
