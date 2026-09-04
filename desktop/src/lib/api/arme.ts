import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Arme,
  ArmeMunitionsConsommation,
  TypeArme,
} from "@/types";

// ========================
// TypeArme API (weapon type catalog)
// ========================

export interface TypeArmePayload {
  nom: string;
  description?: string | null;
  /** Shared ammunition stock for all weapons of this type. */
  munitions_stock?: number;
}

export async function getTypeArmeList(
  search?: string,
): Promise<TypeArme[]> {
  const params = new URLSearchParams();
  if (search) params.set("search", search);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<TypeArme[]>>(
    `/types-armes${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getTypeArmeById(id: number): Promise<TypeArme> {
  const { data } = await apiClient.get<ApiResponse<TypeArme>>(
    `/types-armes/${id}`,
  );
  return data.data;
}

export async function createTypeArme(
  payload: TypeArmePayload,
): Promise<TypeArme> {
  const { data } = await apiClient.post<ApiResponse<TypeArme>>(
    "/types-armes",
    payload,
  );
  return data.data;
}

export async function updateTypeArme(
  id: number,
  payload: Partial<TypeArmePayload>,
): Promise<TypeArme> {
  const { data } = await apiClient.put<ApiResponse<TypeArme>>(
    `/types-armes/${id}`,
    payload,
  );
  return data.data;
}

export async function deleteTypeArme(id: number): Promise<void> {
  await apiClient.delete(`/types-armes/${id}`);
}

// ========================
// Arme API (individual weapon instances + ammunition stock)
// ========================

export interface ArmePayload {
  type_arme_id: number;
  matricule: string;
  munitions_stock?: number;
}

export async function getArmeList(
  filters?: Record<string, string>,
): Promise<Arme[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Arme[]>>(
    `/armes${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getArmeById(id: number): Promise<Arme> {
  const { data } = await apiClient.get<ApiResponse<Arme>>(
    `/armes/${id}`,
  );
  return data.data;
}

export async function createArme(payload: ArmePayload): Promise<Arme> {
  const { data } = await apiClient.post<ApiResponse<Arme>>(
    "/armes",
    payload,
  );
  return data.data;
}

export async function updateArme(
  id: number,
  payload: Partial<ArmePayload>,
): Promise<Arme> {
  const { data } = await apiClient.put<ApiResponse<Arme>>(
    `/armes/${id}`,
    payload,
  );
  return data.data;
}

export async function deleteArme(id: number): Promise<void> {
  await apiClient.delete(`/armes/${id}`);
}

// ========================
// Consommation API (ammunition consumption + history)
// ========================

export interface ConsommationPayload {
  agent_id?: number | null;
  quantite: number;
  armement_id?: number | null;
}

/** Records an ammunition consumption for a specific weapon.
 * The backend atomically decreases the weapon's stock and inserts
 * a consumption history row inside a single transaction. Returns
 * the updated weapon with its new stock value. */
export async function recordConsommation(
  armeId: number,
  payload: ConsommationPayload,
): Promise<Arme> {
  const { data } = await apiClient.post<ApiResponse<Arme>>(
    `/armes/${armeId}/consommation`,
    payload,
  );
  return data.data;
}

/** Returns the ammunition consumption history for a specific weapon. */
export async function getArmeConsommations(
  armeId: number,
): Promise<ArmeMunitionsConsommation[]> {
  const { data } = await apiClient.get<ApiResponse<ArmeMunitionsConsommation[]>>(
    `/armes/${armeId}/consommations`,
  );
  return data.data;
}
