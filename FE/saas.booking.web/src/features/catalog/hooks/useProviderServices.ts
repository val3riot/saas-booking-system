import { FormEvent, useEffect, useMemo, useState } from 'react';
import { apiErrorMessage } from '../../../lib/apiErrors';
import type { Availability, AvailabilityException, OfferedService } from '../../../types/api';
import { toDateTimeLocal } from '../../../utils/dateUtils';
import {
  activateMyAvailability,
  activateMyAvailabilityException,
  activateMyService,
  createMyAvailability,
  createMyAvailabilityException,
  createMyService,
  deactivateMyAvailability,
  deactivateMyAvailabilityException,
  deactivateMyService,
  deleteMyAvailability,
  deleteMyAvailabilityException,
  deleteMyService,
  listMyAvailabilityExceptions,
  listMyAvailabilities,
  listMyServices,
  updateMyAvailability,
  updateMyAvailabilityException,
  updateMyService
} from '../api/providerServicesApi';
import type { AvailabilityFormState, ExceptionFormState, ServiceFormState } from '../types';
import {
  createdExceptionMessage,
  toAvailabilityPayload,
  toExceptionPayloadItems,
  toServicePayload,
  type ExceptionPayloadItem
} from '../utils/providerFormMappers';
import { useProviderProfile } from './useProviderProfile';

type ExceptionConflictState = {
  createdCount: number;
  failedItem: ExceptionPayloadItem;
  message: string;
  remainingItems: ExceptionPayloadItem[];
  savedException: AvailabilityException | null;
};

const emptyForm: ServiceFormState = {
  name: '',
  description: '',
  durationMinutes: '60',
  price: ''
};

const emptyAvailabilityForm: AvailabilityFormState = {
  dayOfWeek: '1',
  startTime: '09:00',
  endTime: '12:00'
};

const emptyExceptionForm: ExceptionFormState = {
  appliesToAll: true,
  serviceIds: [],
  startsAt: '',
  endsAt: '',
  reason: ''
};

export function useProviderServices() {
  const [services, setServices] = useState<OfferedService[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null);
  const [serviceForm, setServiceForm] = useState<ServiceFormState>(emptyForm);
  const [availabilities, setAvailabilities] = useState<Availability[]>([]);
  const [exceptions, setExceptions] = useState<AvailabilityException[]>([]);
  const [availabilityCounts, setAvailabilityCounts] = useState<Record<number, { active: number; total: number }>>({});
  const [selectedAvailabilityId, setSelectedAvailabilityId] = useState<number | null>(null);
  const [selectedExceptionId, setSelectedExceptionId] = useState<number | null>(null);
  const [availabilityForm, setAvailabilityForm] = useState<AvailabilityFormState>(emptyAvailabilityForm);
  const [exceptionForm, setExceptionForm] = useState<ExceptionFormState>(emptyExceptionForm);
  const [exceptionConflict, setExceptionConflict] = useState<ExceptionConflictState | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isSavingAvailability, setIsSavingAvailability] = useState(false);
  const [isSavingException, setIsSavingException] = useState(false);

  const selectedService = useMemo(
    () => services.find((service) => service.id === selectedServiceId) ?? null,
    [selectedServiceId, services]
  );
  const selectedAvailability = useMemo(
    () => availabilities.find((availability) => availability.id === selectedAvailabilityId) ?? null,
    [availabilities, selectedAvailabilityId]
  );
  const selectedException = useMemo(
    () => exceptions.find((exception) => exception.id === selectedExceptionId) ?? null,
    [exceptions, selectedExceptionId]
  );

  const activeServiceCount = services.filter((service) => service.active).length;
  const activeExceptionCount = exceptions.filter((exception) => exception.active).length;
  const providerProfile = useProviderProfile(setError, setMessage);

  useEffect(() => {
    void loadServices();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadServices(nextSelectedId?: number | null) {
    setIsLoading(true);
    setError(null);

    try {
      const providerServices = await listMyServices();
      const providerExceptions = await listMyAvailabilityExceptions();
      setServices(providerServices);
      setExceptions(providerExceptions);
      await loadAvailabilityCounts(providerServices);

      const nextSelectedService =
        nextSelectedId === null
          ? null
          : (providerServices.find((service) => service.id === nextSelectedId) ?? providerServices[0] ?? null);

      await selectService(nextSelectedService, providerServices);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsLoading(false);
    }
  }

  async function selectService(service: OfferedService | null, source = services) {
    setSelectedServiceId(service?.id ?? null);
    setMessage(null);
    setSelectedAvailabilityId(null);
    setAvailabilityForm(emptyAvailabilityForm);
    setSelectedExceptionId(null);
    setExceptionForm(emptyExceptionForm);

    if (!service) {
      setServiceForm(emptyForm);
      setAvailabilities([]);
      return;
    }

    const current = source.find((item) => item.id === service.id) ?? service;
    setServiceForm({
      name: current.name,
      description: current.description ?? '',
      durationMinutes: String(current.durationMinutes),
      price: (current.priceCents / 100).toFixed(2)
    });
    await loadAvailabilities(current.id);
  }

  function startCreateService() {
    setSelectedServiceId(null);
    setServiceForm(emptyForm);
    setAvailabilities([]);
    setSelectedAvailabilityId(null);
    setAvailabilityForm(emptyAvailabilityForm);
    setMessage(null);
    setError(null);
  }

  async function loadAvailabilities(serviceId: number, nextSelectedId?: number | null) {
    setError(null);

    try {
      const serviceAvailabilities = await listMyAvailabilities(serviceId);
      setAvailabilities(serviceAvailabilities);
      setAvailabilityCounts((current) => ({
        ...current,
        [serviceId]: {
          active: serviceAvailabilities.filter((availability) => availability.active).length,
          total: serviceAvailabilities.length
        }
      }));

      const nextSelectedAvailability =
        nextSelectedId === null
          ? null
          : (serviceAvailabilities.find((availability) => availability.id === nextSelectedId) ?? null);
      selectAvailability(nextSelectedAvailability, serviceAvailabilities);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function loadAvailabilityCounts(providerServices: OfferedService[]) {
    const entries = await Promise.all(
      providerServices.map(async (service) => {
        const serviceAvailabilities = await listMyAvailabilities(service.id);
        return [
          service.id,
          {
            active: serviceAvailabilities.filter((availability) => availability.active).length,
            total: serviceAvailabilities.length
          }
        ] as const;
      })
    );

    setAvailabilityCounts(Object.fromEntries(entries));
  }

  function selectAvailability(availability: Availability | null, source = availabilities) {
    setSelectedAvailabilityId(availability?.id ?? null);

    if (!availability) {
      setAvailabilityForm(emptyAvailabilityForm);
      return;
    }

    const current = source.find((item) => item.id === availability.id) ?? availability;
    setAvailabilityForm({
      dayOfWeek: String(current.dayOfWeek),
      startTime: current.startTime.slice(0, 5),
      endTime: current.endTime.slice(0, 5)
    });
  }

  function startCreateAvailability() {
    setSelectedAvailabilityId(null);
    setAvailabilityForm(emptyAvailabilityForm);
    setMessage(null);
    setError(null);
  }

  function selectException(exception: AvailabilityException | null, source = exceptions) {
    setSelectedExceptionId(exception?.id ?? null);

    if (!exception) {
      setExceptionForm(emptyExceptionForm);
      return;
    }

    const current = source.find((item) => item.id === exception.id) ?? exception;
    setExceptionForm({
      appliesToAll: !current.serviceId,
      serviceIds: current.serviceId ? [String(current.serviceId)] : [],
      startsAt: toDateTimeLocal(current.startsAt),
      endsAt: toDateTimeLocal(current.endsAt),
      reason: current.reason ?? ''
    });
  }

  function startCreateException() {
    setSelectedExceptionId(null);
    setExceptionForm({
      ...emptyExceptionForm,
      appliesToAll: !selectedServiceId,
      serviceIds: selectedServiceId ? [String(selectedServiceId)] : []
    });
    setMessage(null);
    setError(null);
  }

  function toggleExceptionService(serviceId: number) {
    const value = String(serviceId);
    setExceptionForm((current) => {
      const nextServiceIds = current.serviceIds.includes(value)
        ? current.serviceIds.filter((item) => item !== value)
        : [...current.serviceIds, value];
      return { ...current, serviceIds: nextServiceIds };
    });
  }

  async function handleServiceSubmit(event: FormEvent) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    setIsSaving(true);

    try {
      const payload = toServicePayload(serviceForm);
      const savedService = selectedService
        ? await updateMyService(selectedService.id, { ...payload, active: selectedService.active })
        : await createMyService(payload);

      setMessage(selectedService ? 'Servizio aggiornato.' : 'Servizio creato.');
      await loadServices(savedService.id);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsSaving(false);
    }
  }

  async function toggleService(service: OfferedService) {
    setMessage(null);
    setError(null);

    try {
      if (service.active) {
        await deactivateMyService(service.id);
        setMessage('Servizio dismesso. Non sara visibile nel catalogo customer.');
      } else {
        await activateMyService(service.id);
        setMessage('Servizio riattivato.');
      }

      await loadServices(service.id);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function removeService(service: OfferedService) {
    setMessage(null);
    setError(null);

    try {
      await deleteMyService(service.id);
      setMessage('Servizio rimosso dal catalogo customer.');
      await loadServices(selectedServiceId === service.id ? null : selectedServiceId);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function handleAvailabilitySubmit(event: FormEvent) {
    event.preventDefault();

    if (!selectedService) {
      return;
    }

    setMessage(null);
    setError(null);
    setIsSavingAvailability(true);

    try {
      const payload = toAvailabilityPayload(availabilityForm);
      const savedAvailability = selectedAvailability
        ? await updateMyAvailability(selectedService.id, selectedAvailability.id, {
            ...payload,
            active: selectedAvailability.active
          })
        : await createMyAvailability(selectedService.id, payload);

      setMessage(selectedAvailability ? 'Disponibilita aggiornata.' : 'Disponibilita creata.');
      await loadAvailabilities(selectedService.id, savedAvailability.id);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsSavingAvailability(false);
    }
  }

  async function toggleAvailability(availability: Availability) {
    if (!selectedService) {
      return;
    }

    setMessage(null);
    setError(null);

    try {
      if (availability.active) {
        await deactivateMyAvailability(selectedService.id, availability.id);
        setMessage('Disponibilita disattivata.');
      } else {
        await activateMyAvailability(selectedService.id, availability.id);
        setMessage('Disponibilita riattivata.');
      }

      await loadAvailabilities(selectedService.id, availability.id);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function removeAvailability(availability: Availability) {
    if (!selectedService) {
      return;
    }

    setMessage(null);
    setError(null);

    try {
      await deleteMyAvailability(selectedService.id, availability.id);
      setMessage('Regola rimossa dalla pianificazione.');
      await loadAvailabilities(selectedService.id, null);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function handleExceptionSubmit(event: FormEvent) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    setExceptionConflict(null);
    setIsSavingException(true);

    try {
      const items = toExceptionPayloadItems(exceptionForm, services);

      if (items.length === 0) {
        setError("Seleziona almeno un servizio oppure applica l'indisponibilita a tutti i servizi.");
        return;
      }

      if (selectedException) {
        const savedException = await updateMyAvailabilityException(selectedException.id, {
          ...items[0].payload,
          active: selectedException.active
        });
        await createExceptionItems(items.slice(1), 1, savedException, 'Indisponibilita aggiornata.');
        return;
      }

      await createExceptionItems(items, 0, null, createdExceptionMessage(items.length));
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsSavingException(false);
    }
  }

  async function createExceptionItems(
    items: ExceptionPayloadItem[],
    initialCreatedCount: number,
    initialSavedException: AvailabilityException | null,
    successMessage: string
  ) {
    let createdCount = initialCreatedCount;
    let savedException = initialSavedException;

    for (let index = 0; index < items.length; index += 1) {
      const item = items[index];

      try {
        const createdException = await createMyAvailabilityException(item.payload);
        savedException = savedException ?? createdException;
        createdCount += 1;
      } catch (requestError) {
        await refreshExceptions(savedException);
        setExceptionConflict({
          createdCount,
          failedItem: item,
          message: apiErrorMessage(requestError),
          remainingItems: items.slice(index + 1),
          savedException
        });
        return;
      }
    }

    await refreshExceptions(savedException);
    setMessage(successMessage);
  }

  async function continueExceptionCreation() {
    if (!exceptionConflict) {
      return;
    }

    const conflict = exceptionConflict;
    setExceptionConflict(null);
    setMessage(null);
    setError(null);

    if (conflict.remainingItems.length === 0) {
      setMessage(createdExceptionMessage(conflict.createdCount));
      return;
    }

    setIsSavingException(true);

    try {
      await createExceptionItems(
        conflict.remainingItems,
        conflict.createdCount,
        conflict.savedException,
        createdExceptionMessage(conflict.createdCount + conflict.remainingItems.length)
      );
    } finally {
      setIsSavingException(false);
    }
  }

  async function refreshExceptions(nextSelectedException: AvailabilityException | null) {
    const providerExceptions = await listMyAvailabilityExceptions();
    setExceptions(providerExceptions);
    selectException(nextSelectedException, providerExceptions);
  }

  async function toggleException(exception: AvailabilityException) {
    setMessage(null);
    setError(null);

    try {
      if (exception.active) {
        await deactivateMyAvailabilityException(exception.id);
        setMessage('Indisponibilita disattivata.');
      } else {
        await activateMyAvailabilityException(exception.id);
        setMessage('Indisponibilita riattivata.');
      }

      const providerExceptions = await listMyAvailabilityExceptions();
      setExceptions(providerExceptions);
      selectException(providerExceptions.find((item) => item.id === exception.id) ?? null, providerExceptions);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function removeException(exception: AvailabilityException) {
    setMessage(null);
    setError(null);

    try {
      await deleteMyAvailabilityException(exception.id);
      setMessage('Indisponibilita rimossa dalla pianificazione.');
      const providerExceptions = await listMyAvailabilityExceptions();
      setExceptions(providerExceptions);
      selectException(null, providerExceptions);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  function serviceRuleCount(serviceId: number) {
    return availabilityCounts[serviceId] ?? { active: 0, total: 0 };
  }

  return {
    activeExceptionCount,
    activeServiceCount,
    availabilities,
    availabilityCounts,
    availabilityForm,
    error,
    exceptionConflict,
    exceptionForm,
    exceptions,
    handleAvailabilitySubmit,
    handleExceptionSubmit,
    handleServiceSubmit,
    isLoading,
    isSaving,
    isSavingAvailability,
    isSavingException,
    message,
    providerProfile,
    removeAvailability,
    removeException,
    removeService,
    selectAvailability,
    selectException,
    selectService,
    selectedAvailability,
    selectedAvailabilityId,
    selectedException,
    selectedExceptionId,
    selectedService,
    selectedServiceId,
    serviceForm,
    serviceRuleCount,
    services,
    setAvailabilityForm,
    setExceptionConflict,
    setExceptionForm,
    setServiceForm,
    startCreateAvailability,
    startCreateException,
    startCreateService,
    toggleAvailability,
    toggleException,
    toggleExceptionService,
    toggleService,
    continueExceptionCreation
  };
}
