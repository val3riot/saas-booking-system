package it.booking.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.auth.AuthRequest;
import it.booking.auth.AuthResponse;
import it.booking.availability.Availability;
import it.booking.availability.AvailabilityExceptionRepository;
import it.booking.availability.AvailabilityRepository;
import it.booking.availability.CreateAvailabilityExceptionRequest;
import it.booking.availability.CreateAvailabilityRequest;
import it.booking.availability.UpdateAvailabilityExceptionRequest;
import it.booking.availability.UpdateAvailabilityRequest;
import it.booking.booking.Booking;
import it.booking.booking.BookingRepository;
import it.booking.offering.CreateOfferedServiceRequest;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.offering.UpdateOfferedServiceRequest;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ProviderRepository providers;

    @Autowired
    private OfferedServiceRepository offeredServices;

    @Autowired
    private AvailabilityRepository availabilities;

    @Autowired
    private AvailabilityExceptionRepository availabilityExceptions;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void providerEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void providerEndpointsRequireAdminRole() throws Exception {
        String customerToken = registerAndGetToken("provider-customer@example.com");

        mockMvc.perform(get("/api/providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void adminCanManageProviders() throws Exception {
        String adminToken = createAdminAndLogin("admin-providers@example.com");
        AppUser providerUser = createUser("provider-owner@example.com", UserRole.PROVIDER);
        CreateProviderRequest createRequest = new CreateProviderRequest(
                providerUser.getId(),
                "Studio Fisio",
                "Fisioterapia e riabilitazione",
                "wellness",
                "Milano",
                "Via Roma 1"
        );

        String createResponse = mockMvc.perform(post("/api/providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessName").value("Studio Fisio"))
                .andExpect(jsonPath("$.userId").value(providerUser.getId()))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProviderResponse created = objectMapper.readValue(createResponse, ProviderResponse.class);

        mockMvc.perform(get("/api/providers/{id}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Milano"));

        UpdateProviderRequest updateRequest = new UpdateProviderRequest(
                providerUser.getId(),
                "Studio Fisio Nord",
                "Fisioterapia sportiva",
                "health",
                "Torino",
                "Via Po 10",
                true
        );

        mockMvc.perform(put("/api/providers/{id}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Studio Fisio Nord"))
                .andExpect(jsonPath("$.city").value("Torino"));

        mockMvc.perform(post("/api/providers/{id}/deactivate", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Provider inactive = providers.findById(created.id()).orElseThrow();
        assertThat(inactive.isActive()).isFalse();

        mockMvc.perform(post("/api/providers/{id}/activate", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Provider active = providers.findById(created.id()).orElseThrow();
        assertThat(active.isActive()).isTrue();

        mockMvc.perform(delete("/api/providers/{id}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Provider deleted = providers.findById(created.id()).orElseThrow();
        assertThat(deleted.isActive()).isFalse();
    }

    @Test
    void createProviderRejectsNonProviderUser() throws Exception {
        String adminToken = createAdminAndLogin("admin-provider-role@example.com");
        AppUser customer = createUser("not-provider@example.com", UserRole.CUSTOMER);
        CreateProviderRequest request = new CreateProviderRequest(
                customer.getId(),
                "Customer Studio",
                null,
                "consulting",
                "Roma",
                null
        );

        mockMvc.perform(post("/api/providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROV_003"));
    }

    @Test
    void createProviderRejectsDuplicateUser() throws Exception {
        String adminToken = createAdminAndLogin("admin-provider-duplicate@example.com");
        AppUser providerUser = createUser("duplicate-provider@example.com", UserRole.PROVIDER);
        providers.save(new Provider(providerUser, "Existing Provider", null, "wellness", "Milano", null));
        CreateProviderRequest request = new CreateProviderRequest(
                providerUser.getId(),
                "Duplicate Provider",
                null,
                "wellness",
                "Milano",
                null
        );

        mockMvc.perform(post("/api/providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROV_002"));
    }

    @Test
    void providerCanManageOwnProfile() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-profile@example.com", "Owner Studio");

        mockMvc.perform(get("/api/providers/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.provider().getId()))
                .andExpect(jsonPath("$.businessName").value("Owner Studio"));

        UpdateProviderProfileRequest request = new UpdateProviderProfileRequest(
                "Owner Studio Plus",
                "Profilo aggiornato",
                "consulting",
                "Bologna",
                "Via Verdi 3"
        );

        mockMvc.perform(put("/api/providers/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Owner Studio Plus"))
                .andExpect(jsonPath("$.city").value("Bologna"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void providerSelfEndpointsRequireProviderRole() throws Exception {
        String customerToken = registerAndGetToken("provider-self-customer@example.com");

        mockMvc.perform(get("/api/providers/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void providerCanCreateOwnProfileWhenMissing() throws Exception {
        String token = createProviderUserAndLogin("provider-create-profile@example.com");
        CreateProviderProfileRequest request = new CreateProviderProfileRequest(
                "New Provider Studio",
                "Profilo creato dal provider",
                "wellness",
                "Napoli",
                "Via Toledo 10"
        );

        mockMvc.perform(post("/api/providers/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessName").value("New Provider Studio"))
                .andExpect(jsonPath("$.city").value("Napoli"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/providers/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROV_002"));
    }

    @Test
    void providerCanManageOwnServices() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-services@example.com", "Service Studio");
        CreateOfferedServiceRequest createRequest = new CreateOfferedServiceRequest(
                "Consulenza",
                "Sessione iniziale",
                60,
                5000
        );

        String createResponse = mockMvc.perform(post("/api/providers/me/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(session.provider().getId()))
                .andExpect(jsonPath("$.name").value("Consulenza"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long serviceId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/providers/me/services/{serviceId}", serviceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId))
                .andExpect(jsonPath("$.name").value("Consulenza"));

        mockMvc.perform(get("/api/providers/me/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Consulenza"));

        UpdateOfferedServiceRequest updateRequest = new UpdateOfferedServiceRequest(
                "Consulenza avanzata",
                "Sessione avanzata",
                90,
                7500,
                true
        );

        mockMvc.perform(put("/api/providers/me/services/{serviceId}", serviceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consulenza avanzata"))
                .andExpect(jsonPath("$.durationMinutes").value(90));

        mockMvc.perform(post("/api/providers/me/services/{serviceId}/deactivate", serviceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());

        OfferedService inactive = offeredServices.findById(serviceId).orElseThrow();
        assertThat(inactive.isActive()).isFalse();

        mockMvc.perform(post("/api/providers/me/services/{serviceId}/activate", serviceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());

        OfferedService active = offeredServices.findById(serviceId).orElseThrow();
        assertThat(active.isActive()).isTrue();

        mockMvc.perform(delete("/api/providers/me/services/{serviceId}", serviceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void providerServiceRejectsDuplicateName() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-service-duplicate@example.com", "Duplicate Studio");
        CreateOfferedServiceRequest request = new CreateOfferedServiceRequest("Checkup", null, 45, 3000);

        mockMvc.perform(post("/api/providers/me/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/providers/me/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERV_002"));
    }

    @Test
    void providerCannotAccessAnotherProviderService() throws Exception {
        ProviderSession owner = createProviderAndLogin("owner-service-a@example.com", "Owner A");
        ProviderSession other = createProviderAndLogin("owner-service-b@example.com", "Owner B");
        OfferedService service = offeredServices.save(new OfferedService(owner.provider(), "Privato", null, 30, 2500));

        mockMvc.perform(get("/api/providers/me/services/{serviceId}", service.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERV_001"));

        mockMvc.perform(put("/api/providers/me/services/{serviceId}", service.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOfferedServiceRequest(
                                "Tentativo",
                                null,
                                30,
                                2500,
                                true
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERV_001"));
    }

    @Test
    void providerServiceRejectsInvalidPayload() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-invalid-service@example.com", "Invalid Service Studio");
        CreateOfferedServiceRequest request = new CreateOfferedServiceRequest("", null, 0, -1);

        mockMvc.perform(post("/api/providers/me/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.name.code").value("VAL_001"))
                .andExpect(jsonPath("$.fields.durationMinutes.code").value("VAL_002"))
                .andExpect(jsonPath("$.fields.priceCents.code").value("VAL_002"));
    }

    @Test
    void providerCanManageOwnAvailabilities() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-availabilities@example.com", "Availability Studio");
        OfferedService service = offeredServices.save(new OfferedService(session.provider(), "Visita", null, 60, 5000));
        CreateAvailabilityRequest createRequest = new CreateAvailabilityRequest(
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)
        );

        String createResponse = mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities", service.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(session.provider().getId()))
                .andExpect(jsonPath("$.serviceId").value(service.getId()))
                .andExpect(jsonPath("$.dayOfWeek").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long availabilityId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}", service.getId(), availabilityId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(availabilityId))
                .andExpect(jsonPath("$.startTime").value("09:00:00"));

        mockMvc.perform(get("/api/providers/me/services/{serviceId}/availabilities", service.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(availabilityId));

        mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities", service.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAvailabilityRequest(
                                (short) 1,
                                LocalTime.of(11, 0),
                                LocalTime.of(13, 0)
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AVAIL_003"));

        mockMvc.perform(put("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}", service.getId(), availabilityId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAvailabilityRequest(
                                (short) 1,
                                LocalTime.of(13, 0),
                                LocalTime.of(12, 0),
                                true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AVAIL_002"));

        mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}/deactivate", service.getId(), availabilityId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());

        Availability inactive = availabilities.findById(availabilityId).orElseThrow();
        assertThat(inactive.isActive()).isFalse();

        mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}/activate", service.getId(), availabilityId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}", service.getId(), availabilityId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void providerCanDefineDifferentCalendarsForDifferentServices() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-service-calendars@example.com", "Calendar Studio");
        OfferedService firstService = offeredServices.save(new OfferedService(session.provider(), "Prima visita", null, 60, 5000));
        OfferedService secondService = offeredServices.save(new OfferedService(session.provider(), "Controllo", null, 30, 3000));
        CreateAvailabilityRequest mondayMorning = new CreateAvailabilityRequest(
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)
        );

        String firstResponse = mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities", firstService.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mondayMorning)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value(firstService.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long firstAvailabilityId = objectMapper.readTree(firstResponse).get("id").asLong();

        String secondResponse = mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities", secondService.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mondayMorning)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value(secondService.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long secondAvailabilityId = objectMapper.readTree(secondResponse).get("id").asLong();

        mockMvc.perform(get("/api/providers/me/services/{serviceId}/availabilities", firstService.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstAvailabilityId));

        mockMvc.perform(get("/api/providers/me/services/{serviceId}/availabilities", secondService.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondAvailabilityId));
    }

    @Test
    void providerCannotAccessAnotherProviderAvailability() throws Exception {
        ProviderSession owner = createProviderAndLogin("owner-availability-a@example.com", "Availability A");
        ProviderSession other = createProviderAndLogin("owner-availability-b@example.com", "Availability B");
        OfferedService ownerService = offeredServices.save(new OfferedService(owner.provider(), "Privato", null, 60, 5000));
        OfferedService otherService = offeredServices.save(new OfferedService(other.provider(), "Altro", null, 60, 5000));
        Availability availability = availabilities.save(new Availability(
                owner.provider(),
                ownerService,
                (short) 2,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        ));

        mockMvc.perform(get("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}", otherService.getId(), availability.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AVAIL_001"));

        mockMvc.perform(delete("/api/providers/me/services/{serviceId}/availabilities/{availabilityId}", otherService.getId(), availability.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AVAIL_001"));
    }

    @Test
    void providerAvailabilityRejectsInvalidPayload() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-invalid-availability@example.com", "Invalid Availability Studio");
        OfferedService service = offeredServices.save(new OfferedService(session.provider(), "Invalid", null, 60, 5000));
        CreateAvailabilityRequest request = new CreateAvailabilityRequest((short) 8, null, null);

        mockMvc.perform(post("/api/providers/me/services/{serviceId}/availabilities", service.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.dayOfWeek.code").value("VAL_002"))
                .andExpect(jsonPath("$.fields.startTime.code").value("VAL_001"))
                .andExpect(jsonPath("$.fields.endTime.code").value("VAL_001"));
    }

    @Test
    void providerCanManageAvailabilityExceptions() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-exception@example.com", "Exception Studio");
        OfferedService service = offeredServices.save(new OfferedService(session.provider(), "Visita", null, 60, 5000));
        Instant startsAt = Instant.parse("2026-06-08T10:00:00Z");
        Instant endsAt = Instant.parse("2026-06-08T11:00:00Z");

        String createResponse = mockMvc.perform(post("/api/providers/me/availability-exceptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAvailabilityExceptionRequest(
                                service.getId(),
                                startsAt,
                                endsAt,
                                "Permesso"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(session.provider().getId()))
                .andExpect(jsonPath("$.serviceId").value(service.getId()))
                .andExpect(jsonPath("$.reason").value("Permesso"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long exceptionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/providers/me/availability-exceptions/{exceptionId}", exceptionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exceptionId));

        mockMvc.perform(get("/api/providers/me/availability-exceptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(exceptionId));

        mockMvc.perform(put("/api/providers/me/availability-exceptions/{exceptionId}", exceptionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAvailabilityExceptionRequest(
                                null,
                                startsAt,
                                Instant.parse("2026-06-08T12:00:00Z"),
                                "Chiusura studio",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").doesNotExist())
                .andExpect(jsonPath("$.reason").value("Chiusura studio"));

        mockMvc.perform(post("/api/providers/me/availability-exceptions/{exceptionId}/deactivate", exceptionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());

        assertThat(availabilityExceptions.findById(exceptionId).orElseThrow().isActive()).isFalse();

        mockMvc.perform(post("/api/providers/me/availability-exceptions/{exceptionId}/activate", exceptionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/providers/me/availability-exceptions/{exceptionId}", exceptionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void providerAvailabilityExceptionRejectsOverlapsAndInvalidInterval() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-exception-overlap@example.com", "Exception Overlap Studio");
        OfferedService service = offeredServices.save(new OfferedService(session.provider(), "Visita", null, 60, 5000));
        Instant startsAt = Instant.parse("2026-06-08T10:00:00Z");
        Instant endsAt = Instant.parse("2026-06-08T11:00:00Z");

        mockMvc.perform(post("/api/providers/me/availability-exceptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAvailabilityExceptionRequest(
                                service.getId(),
                                startsAt,
                                endsAt,
                                "Primo blocco"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/providers/me/availability-exceptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAvailabilityExceptionRequest(
                                service.getId(),
                                Instant.parse("2026-06-08T10:30:00Z"),
                                Instant.parse("2026-06-08T11:30:00Z"),
                                "Overlap"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AVEX_003"));

        mockMvc.perform(post("/api/providers/me/availability-exceptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAvailabilityExceptionRequest(
                                service.getId(),
                                endsAt,
                                startsAt,
                                "Intervallo errato"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AVEX_002"));
    }

    @Test
    void providerAvailabilityExceptionRejectsActiveBookingOverlap() throws Exception {
        ProviderSession session = createProviderAndLogin("owner-exception-booking@example.com", "Exception Booking Studio");
        OfferedService service = offeredServices.save(new OfferedService(session.provider(), "Visita", null, 60, 5000));
        AppUser customer = createUser("owner-exception-booking-customer@example.com", UserRole.CUSTOMER);
        Instant startsAt = Instant.parse("2026-06-09T10:00:00Z");
        bookings.save(new Booking(
                customer,
                session.provider(),
                service,
                startsAt,
                startsAt.plusSeconds(3600)
        ));

        mockMvc.perform(post("/api/providers/me/availability-exceptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAvailabilityExceptionRequest(
                                service.getId(),
                                Instant.parse("2026-06-09T10:30:00Z"),
                                Instant.parse("2026-06-09T11:30:00Z"),
                                "Indisponibilità su prenotazione esistente"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AVEX_004"));
    }

    @Test
    void providerSelfFeaturesRequireExistingProviderProfile() throws Exception {
        String token = createProviderUserAndLogin("provider-without-profile@example.com");

        mockMvc.perform(get("/api/providers/me/services")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROV_001"));
    }

    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "Password1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private String createAdminAndLogin(String email) throws Exception {
        createUser(email, UserRole.ADMIN);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "Password1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private ProviderSession createProviderAndLogin(String email, String businessName) throws Exception {
        AppUser user = createUser(email, UserRole.PROVIDER);
        Provider provider = providers.save(new Provider(user, businessName, null, "wellness", "Milano", null));
        String token = login(email);
        return new ProviderSession(user, provider, token);
    }

    private String createProviderUserAndLogin(String email) throws Exception {
        createUser(email, UserRole.PROVIDER);
        return login(email);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "Password1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private AppUser createUser(String email, UserRole role) {
        return users.save(new AppUser(email, passwordEncoder.encode("Password1!"), role));
    }

    private record ProviderSession(AppUser user, Provider provider, String token) {
    }
}
