import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPlainteEntreeList,
  getPlainteSortieList,
  deletePlainteEntree,
  deletePlainteSortie,
  PLAINTE_ENTREE_TYPES,
  PLAINTE_ENTREE_TYPE_LABELS,
  PLAINTE_SORTIE_NATURES,
  PLAINTE_SORTIE_NATURE_LABELS,
} from "@/lib/api/plainte";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Scale, Pencil, Trash2, ArrowRightLeft } from "lucide-react";
import type { PlainteEntree, PlainteSortie } from "@/types";

const PJ_PLAINTE_MODULE = "pj_plainte";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

type Tab = "entree" | "sortie";

export function PlainteList() {
  const [tab, setTab] = useState<Tab>("entree");
  const [entrees, setEntrees] = useState<PlainteEntree[]>([]);
  const [sorties, setSorties] = useState<PlainteSortie[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [natureFilter, setNatureFilter] = useState("");
  const [deleteTargetEntree, setDeleteTargetEntree] = useState<PlainteEntree | null>(null);
  const [deleteTargetSortie, setDeleteTargetSortie] = useState<PlainteSortie | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_PLAINTE_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_PLAINTE_MODULE, "can_delete");

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadAll() {
    setLoading(true);
    try {
      const [entreeData, sortieData] = await Promise.all([
        getPlainteEntreeList().catch(() => []),
        getPlainteSortieList().catch(() => []),
      ]);
      setEntrees(entreeData);
      setSorties(sortieData);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les plaintes");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteTargetEntree) {
        await deletePlainteEntree(deleteTargetEntree.id);
        addNotification("success", "Supprimée", "Plainte supprimée avec succès");
      } else if (deleteTargetSortie) {
        await deletePlainteSortie(deleteTargetSortie.id);
        addNotification("success", "Supprimée", "Sortie supprimée avec succès");
      }
      loadAll();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteTargetEntree(null);
      setDeleteTargetSortie(null);
    }
  }

  const filteredEntrees = entrees.filter((e) => {
    if (typeFilter && e.type !== typeFilter) return false;
    return true;
  });

  const filteredSorties = sorties.filter((s) => {
    if (natureFilter && s.nature !== natureFilter) return false;
    return true;
  });

  const entreeColumns: Column<PlainteEntree>[] = [
    {
      key: "type",
      header: "Type",
      sortable: true,
      render: (e) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
          {PLAINTE_ENTREE_TYPE_LABELS[e.type] ?? e.type}
        </span>
      ),
    },
    {
      key: "numero_dossier",
      header: "N° Dossier",
      sortable: true,
      render: (e) => <span className="font-mono text-xs font-semibold">{e.numero_dossier}</span>,
    },
    {
      key: "date_plainte",
      header: "Date",
      sortable: true,
      render: (e) => formatDate(e.date_plainte),
    },
    {
      key: "partie_civile",
      header: "Partie civile",
      render: (e) => e.partie_civile ?? "—",
    },
    {
      key: "mise_en_cause",
      header: "Mise en cause",
      render: (e) => e.mise_en_cause ?? "—",
    },
    {
      key: "infraction",
      header: "Infraction",
      render: (e) => <span className="line-clamp-1 max-w-xs">{e.infraction ?? "—"}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[120px]",
      render: (e) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          {canCreate && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Créer une sortie"
              onClick={() => navigate(`/pj/plainte/sortie/new?entreeId=${e.id}`)}
            >
              <ArrowRightLeft className="h-3.5 w-3.5" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/plainte/${e.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTargetEntree(e)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  const sortieColumns: Column<PlainteSortie>[] = [
    {
      key: "nature",
      header: "Nature",
      sortable: true,
      render: (s) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-secondary/10 text-secondary-foreground font-medium">
          {PLAINTE_SORTIE_NATURE_LABELS[s.nature] ?? s.nature}
        </span>
      ),
    },
    {
      key: "numero",
      header: "N° Sortie",
      sortable: true,
      render: (s) => <span className="font-mono text-xs font-semibold">{s.numero}</span>,
    },
    {
      key: "date_sortie",
      header: "Date",
      sortable: true,
      render: (s) => formatDate(s.date_sortie),
    },
    {
      key: "entree_numero_dossier",
      header: "Entrée",
      render: (s) => s.entree_numero_dossier ?? "—",
    },
    {
      key: "numero_ttr",
      header: "N° TTR",
      render: (s) => s.numero_ttr ?? "—",
    },
    {
      key: "nom_substitut",
      header: "Substitut",
      render: (s) => s.nom_substitut ?? "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (s) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/plainte/sortie/${s.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTargetSortie(s)}
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
          <h1 className="text-2xl font-semibold tracking-tight">Plainte</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Police Judiciaire — Plaintes ENTRÉE et SORTIE
          </p>
        </div>
        {canCreate && (
          <Button
            onClick={() => navigate(tab === "entree" ? "/pj/plainte/new" : "/pj/plainte/sortie/new")}
            className="gap-2"
          >
            <Plus className="h-4 w-4" />
            {tab === "entree" ? "Nouvelle plainte" : "Nouvelle sortie"}
          </Button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b">
        <button
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            tab === "entree"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
          onClick={() => setTab("entree")}
        >
          ENTRÉE ({entrees.length})
        </button>
        <button
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            tab === "sortie"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
          onClick={() => setTab("sortie")}
        >
          SORTIE ({sorties.length})
        </button>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <CardTitle className="text-base flex items-center gap-2">
              <Scale className="h-4 w-4" />
              {tab === "entree" ? "Plaintes ENTRÉE" : "Sorties"}
              {tab === "entree"
                ? ` (${filteredEntrees.length})`
                : ` (${filteredSorties.length})`}
            </CardTitle>
            <div className="flex items-center gap-2">
              {tab === "entree" ? (
                <Select
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value)}
                  options={[
                    { value: "", label: "Tous les types" },
                    ...PLAINTE_ENTREE_TYPES.map((t) => ({
                      value: t,
                      label: PLAINTE_ENTREE_TYPE_LABELS[t],
                    })),
                  ]}
                  className="w-48"
                />
              ) : (
                <Select
                  value={natureFilter}
                  onChange={(e) => setNatureFilter(e.target.value)}
                  options={[
                    { value: "", label: "Toutes les natures" },
                    ...PLAINTE_SORTIE_NATURES.map((n) => ({
                      value: n,
                      label: PLAINTE_SORTIE_NATURE_LABELS[n],
                    })),
                  ]}
                  className="w-48"
                />
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {tab === "entree" ? (
            <DataTable
              columns={entreeColumns}
              data={filteredEntrees}
              keyExtractor={(e) => e.id}
              loading={loading}
              searchable
              searchPlaceholder="Rechercher par dossier, PC, MC, infraction..."
              emptyMessage="Aucune plainte ENTRÉE"
              onRowClick={(e) => navigate(`/pj/plainte/${e.id}`)}
            />
          ) : (
            <DataTable
              columns={sortieColumns}
              data={filteredSorties}
              keyExtractor={(s) => s.id}
              loading={loading}
              searchable
              searchPlaceholder="Rechercher par numéro, TTR, substitut..."
              emptyMessage="Aucune sortie"
              onRowClick={(s) => navigate(`/pj/plainte/sortie/${s.id}`)}
            />
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTargetEntree !== null}
        title="Supprimer la plainte"
        message={`Voulez-vous vraiment supprimer la plainte « ${deleteTargetEntree?.numero_dossier} » ? La sortie associée et les pièces jointes seront également supprimées.`}
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTargetEntree(null)}
      />

      <ConfirmDialog
        open={deleteTargetSortie !== null}
        title="Supprimer la sortie"
        message={`Voulez-vous vraiment supprimer la sortie « ${deleteTargetSortie?.numero} » ?`}
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTargetSortie(null)}
      />
    </motion.div>
  );
}
