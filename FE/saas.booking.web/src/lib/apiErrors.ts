import { ApiError } from '../services/http/apiClient';

export function apiErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    const fieldMessages = Object.values(error.response.fields ?? {}).map((field) => field.message);
    return [localizedApiMessage(error), ...fieldMessages].filter(Boolean).join(' ');
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Operazione non riuscita.';
}

function localizedApiMessage(error: ApiError) {
  if (error.response.code === 'AVEX_003') {
    return 'Esiste gia una indisponibilita attiva nello stesso intervallo.';
  }

  if (error.response.code === 'AVEX_004') {
    return 'Non puoi creare questa indisponibilita perche si sovrappone a una prenotazione attiva.';
  }

  if (error.response.code === 'BOOK_003') {
    return 'Orario selezionato non disponibile.';
  }

  return error.response.message;
}
