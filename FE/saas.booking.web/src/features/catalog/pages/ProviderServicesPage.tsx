import { StatusMessage } from '../../../components/StatusMessage';
import { ExceptionConflictModal } from '../components/ExceptionConflictModal';
import { ProviderProfileSection } from '../components/ProviderProfileSection';
import { ProviderServicesHeader } from '../components/ProviderServicesHeader';
import { ProviderUnavailabilitySection } from '../components/ProviderUnavailabilitySection';
import { ServiceEditorSection } from '../components/ServiceEditorSection';
import { ServiceList } from '../components/ServiceList';
import { useProviderServices } from '../hooks/useProviderServices';

export function ProviderServicesPage() {
  const providerServices = useProviderServices();

  return (
    <div className="mx-auto grid max-w-6xl gap-5">
      <ProviderServicesHeader
        activeExceptionCount={providerServices.activeExceptionCount}
        activeServiceCount={providerServices.activeServiceCount}
        onCreateService={providerServices.startCreateService}
        totalServiceCount={providerServices.services.length}
      />

      {providerServices.error && <StatusMessage tone="danger">{providerServices.error}</StatusMessage>}
      {providerServices.message && <StatusMessage tone="success">{providerServices.message}</StatusMessage>}

      <ProviderProfileSection
        form={providerServices.providerProfile.profileForm}
        isSaving={providerServices.providerProfile.isSavingProfile}
        onChange={providerServices.providerProfile.setProfileForm}
        onSubmit={providerServices.providerProfile.handleProfileSubmit}
        profile={providerServices.providerProfile.profile}
      />

      <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.9fr)_minmax(0,1.4fr)]">
        <ServiceList
          availabilityCounts={providerServices.availabilityCounts}
          isLoading={providerServices.isLoading}
          onSelect={(service) => void providerServices.selectService(service)}
          selectedServiceId={providerServices.selectedServiceId}
          services={providerServices.services}
        />
        <ServiceEditorSection
          availabilities={providerServices.availabilities}
          availabilityForm={providerServices.availabilityForm}
          isSaving={providerServices.isSaving}
          isSavingAvailability={providerServices.isSavingAvailability}
          onAvailabilityFormChange={providerServices.setAvailabilityForm}
          onAvailabilitySubmit={providerServices.handleAvailabilitySubmit}
          onCreateAvailability={providerServices.startCreateAvailability}
          onFormChange={providerServices.setServiceForm}
          onRemoveAvailability={(availability) => void providerServices.removeAvailability(availability)}
          onRemoveService={(service) => void providerServices.removeService(service)}
          onSelectAvailability={providerServices.selectAvailability}
          onServiceSubmit={providerServices.handleServiceSubmit}
          onToggleAvailability={(availability) => void providerServices.toggleAvailability(availability)}
          onToggleService={(service) => void providerServices.toggleService(service)}
          selectedAvailability={providerServices.selectedAvailability}
          selectedAvailabilityId={providerServices.selectedAvailabilityId}
          selectedService={providerServices.selectedService}
          serviceForm={providerServices.serviceForm}
          serviceRuleCount={
            providerServices.selectedService
              ? providerServices.serviceRuleCount(providerServices.selectedService.id)
              : { active: 0, total: 0 }
          }
        />
      </div>

      <ProviderUnavailabilitySection
        exceptionForm={providerServices.exceptionForm}
        exceptions={providerServices.exceptions}
        isSavingException={providerServices.isSavingException}
        onCreate={providerServices.startCreateException}
        onFormChange={providerServices.setExceptionForm}
        onRemove={(exception) => void providerServices.removeException(exception)}
        onSelect={providerServices.selectException}
        onSubmit={providerServices.handleExceptionSubmit}
        onToggle={(exception) => void providerServices.toggleException(exception)}
        onToggleService={providerServices.toggleExceptionService}
        selectedException={providerServices.selectedException}
        selectedExceptionId={providerServices.selectedExceptionId}
        services={providerServices.services}
      />

      {providerServices.exceptionConflict && (
        <ExceptionConflictModal
          conflict={providerServices.exceptionConflict}
          isSaving={providerServices.isSavingException}
          onClose={() => providerServices.setExceptionConflict(null)}
          onContinue={() => void providerServices.continueExceptionCreation()}
        />
      )}
    </div>
  );
}
