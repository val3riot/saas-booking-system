import { FormEvent, useEffect, useState } from 'react';
import { apiErrorMessage } from '../../../lib/apiErrors';
import type { ProviderProfile } from '../../../types/api';
import { createMyProviderProfile, getMyProviderProfile, updateMyProviderProfile } from '../api/providerServicesApi';
import type { ProviderProfileFormState } from '../types';
import {
  emptyProviderProfileForm,
  toProviderProfileForm,
  toProviderProfilePayload
} from '../utils/providerFormMappers';

export function useProviderProfile(onError: (message: string) => void, onMessage: (message: string) => void) {
  const [profile, setProfile] = useState<ProviderProfile | null>(null);
  const [profileForm, setProfileForm] = useState<ProviderProfileFormState>(emptyProviderProfileForm);
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  useEffect(() => {
    void loadProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadProfile() {
    try {
      const providerProfile = await getMyProviderProfile();
      setProfile(providerProfile);
      setProfileForm(toProviderProfileForm(providerProfile));
    } catch (requestError) {
      onError(apiErrorMessage(requestError));
    }
  }

  async function handleProfileSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSavingProfile(true);

    try {
      const payload = toProviderProfilePayload(profileForm);
      const savedProfile = profile ? await updateMyProviderProfile(payload) : await createMyProviderProfile(payload);
      setProfile(savedProfile);
      setProfileForm(toProviderProfileForm(savedProfile));
      onMessage('Profilo provider aggiornato.');
    } catch (requestError) {
      onError(apiErrorMessage(requestError));
    } finally {
      setIsSavingProfile(false);
    }
  }

  return {
    handleProfileSubmit,
    isSavingProfile,
    profile,
    profileForm,
    setProfileForm
  };
}
