import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  TypeMateriel,
  AffectationMateriel,
} from "@/types";

// ========================
// Type Matériel API (equipment type catalog)
// ========================

export interface TypeMaterielPayload {
  nom: string;
  description?: string | null;
}

export async function getTypeMaterielList(
  filters?: Record<string, string>,
): Promise<TypeMateriel[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<TypeMateriel[]>>(
    `/types-materiels${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getTypeMaterielById(
  id: number,
): Promise<TypeMateriel> {
  const { data } = await apiClient.get<ApiResponse<TypeMateriel>>(
    `/types-materiels/${id}`,
  );
  return data.data;
}

export async function createTypeMateriel(
  payload: TypeMaterielPayload,
): Promise<TypeMateriel> {
  const { data } = await apiClient.post<ApiResponse<TypeMateriel>>(
    "/types-materiels",
    payload,
  );
  return data.data;
}

export async function updateTypeMateriel(
  id: number,
  payload: Partial<TypeMaterielPayload>,
): Promise<TypeMateriel> {
  const { data } = await apiClient.put<ApiResponse<TypeMateriel>>(
    `/types-materiels/${id}`,
    payload,
  );
  return data.data;
}

export async function deleteTypeMateriel(id: number): Promise<void> {
  await apiClient.delete(`/types-materiels/${id}`);
}

// ========================
// Affectation Matériel API (equipment assignment & return)
// ========================

export interface AffectationMaterielLignePayload {
  type_materiel_id: number;
  numero_materiel: string;
  etat_emport?: string | null;
}

export interface AffectationMaterielPayload {
  agent_personnel_id: number;
  date_perception: string;
  heure_perception: string;
  observations?: string | null;
  lignes: AffectationMaterielLignePayload[];
  code_secret?: string;
  signature_svg?: string | null;
}

export interface ReintegrationMaterielPayload {
  date_reintegration: string;
  heure_reintegration: string;
  /** Map of ligneId → etat_reintegration (one per material). */
  ligne_etats: Record<number, string>;
}

export async function getAffectationMaterielList(
  filters?: Record<string, string>,
): Promise<AffectationMateriel[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<AffectationMateriel[]>>(
    `/affectations-materiels${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getAffectationMaterielById(
  id: number,
): Promise<AffectationMateriel> {
  const { data } = await apiClient.get<ApiResponse<AffectationMateriel>>(
    `/affectations-materiels/${id}`,
  );
  return data.data;
}

export async function createAffectationMateriel(
  payload: AffectationMaterielPayload,
): Promise<AffectationMateriel> {
  const { data } = await apiClient.post<ApiResponse<AffectationMateriel>>(
    "/affectations-materiels",
    payload,
  );
  return data.data;
}

export async function updateAffectationMateriel(
  id: number,
  payload: Partial<AffectationMaterielPayload>,
): Promise<AffectationMateriel> {
  const { data } = await apiClient.put<ApiResponse<AffectationMateriel>>(
    `/affectations-materiels/${id}`,
    payload,
  );
  return data.data;
}

export async function reintegrateAffectationMateriel(
  id: number,
  payload: ReintegrationMaterielPayload,
): Promise<AffectationMateriel> {
  const { data } = await apiClient.post<ApiResponse<AffectationMateriel>>(
    `/affectations-materiels/${id}/reintegration`,
    payload,
  );
  return data.data;
}

export async function deleteAffectationMateriel(id: number): Promise<void> {
  await apiClient.delete(`/affectations-materiels/${id}`);
}
