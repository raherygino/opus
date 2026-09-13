import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  MaterielRoulant,
  MaterielRoulantType,
} from "@/types";

// ========================
// Matériel Roulant API (vehicle perception & reintegration — VHL / Moto)
// ========================

export interface MaterielRoulantPayload {
  date_perception: string;
  heure_perception: string;
  type_materiel: MaterielRoulantType;
  numero_immatriculation?: string | null;
  description_vehicule?: string | null;
  agent_conducteur_personnel_id: number;
  chef_de_bord_personnel_id?: number | null;
  kilometrage_depart?: number | string | null;
  niveau_carburant_depart?: number | string | null;
  // Agent verification (create only — set at perception time, one-way).
  code_secret?: string;
  signature_svg?: string | null;
}

export interface ReintegrationMaterielRoulantPayload {
  date_reintegration: string;
  heure_reintegration: string;
  kilometrage_retour: number | string;
  niveau_carburant_retour?: number | string | null;
  observations_techniques?: string | null;
  defaillances?: string | null;
}

export async function getMaterielRoulantList(
  filters?: Record<string, string>,
): Promise<MaterielRoulant[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<MaterielRoulant[]>>(
    `/materiels-roulants${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getMaterielRoulantById(
  id: number,
): Promise<MaterielRoulant> {
  const { data } = await apiClient.get<ApiResponse<MaterielRoulant>>(
    `/materiels-roulants/${id}`,
  );
  return data.data;
}

export async function createMaterielRoulant(
  payload: MaterielRoulantPayload,
): Promise<MaterielRoulant> {
  const { data } = await apiClient.post<ApiResponse<MaterielRoulant>>(
    "/materiels-roulants",
    payload,
  );
  return data.data;
}

export async function updateMaterielRoulant(
  id: number,
  payload: Partial<MaterielRoulantPayload>,
): Promise<MaterielRoulant> {
  const { data } = await apiClient.put<ApiResponse<MaterielRoulant>>(
    `/materiels-roulants/${id}`,
    payload,
  );
  return data.data;
}

export async function reintegrateMaterielRoulant(
  id: number,
  payload: ReintegrationMaterielRoulantPayload,
): Promise<MaterielRoulant> {
  const { data } = await apiClient.post<ApiResponse<MaterielRoulant>>(
    `/materiels-roulants/${id}/reintegration`,
    payload,
  );
  return data.data;
}

export async function deleteMaterielRoulant(id: number): Promise<void> {
  await apiClient.delete(`/materiels-roulants/${id}`);
}
