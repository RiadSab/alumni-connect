// Employment history endpoints. /me is candidate-only; /confirm is public (token in the link).

import { http } from "@/lib/http";
import type {
  EmploymentConfirmDetailsDTO,
  EmploymentEntryDTO,
  SaveEmploymentEntryInput,
} from "@/types/employment";

export const employmentApi = {
  mine: () => http.get<EmploymentEntryDTO[]>("/employment/me"),

  create: (body: SaveEmploymentEntryInput) =>
    http.post<EmploymentEntryDTO>("/employment/me", { json: body }),

  update: (id: number, body: SaveEmploymentEntryInput) =>
    http.patch<EmploymentEntryDTO>(`/employment/me/${id}`, { json: body }),

  remove: (id: number) => http.del<void>(`/employment/me/${id}`),

  confirmDetails: (token: string) =>
    http.get<EmploymentConfirmDetailsDTO>(`/employment/confirm/${token}`),

  confirm: (token: string) => http.post<void>(`/employment/confirm/${token}`),
};
