package it.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.audit.AuditEntityType;
import it.booking.audit.AuditEventType;
import it.booking.audit.AuditLogRepository;
import it.booking.auth.AuthRequest;
import it.booking.auth.AuthResponse;
import it.booking.availability.Availability;
import it.booking.availability.AvailabilityException;
import it.booking.availability.AvailabilityExceptionRepository;
import it.booking.availability.AvailabilityRepository;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
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
class BookingControllerIntegrationTest {

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
    private AuditLogRepository auditLogs;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void bookingEndpointsRequireCustomerRole() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));

        ProviderFixture provider = createProviderFixture("booking-provider-role@example.com");
        String providerToken = login(provider.user().getEmail());

        mockMvc.perform(get("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void customerCanCreateListGetAndCancelBooking() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-customer@example.com");
        Instant startsAt = nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0));
        CreateBookingRequest request = new CreateBookingRequest(
                provider.provider().getId(),
                provider.service().getId(),
                startsAt
        );

        String createResponse = mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(provider.provider().getId()))
                .andExpect(jsonPath("$.serviceId").value(provider.service().getId()))
                .andExpect(jsonPath("$.serviceName").value("Prima visita"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        BookingResponse created = objectMapper.readValue(createResponse, BookingResponse.class);
        assertThat(created.endsAt()).isEqualTo(startsAt.plusSeconds(60 * 60));

        mockMvc.perform(get("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(created.id()));

        mockMvc.perform(get("/api/bookings/{bookingId}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));

        mockMvc.perform(post("/api/bookings/{bookingId}/cancel", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        Booking cancelled = bookings.findById(created.id()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancelledBy().getId()).isEqualTo(created.customerId());
        assertThat(cancelled.getCancellationReason()).isNull();
        assertThat(auditLogs.findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(AuditEntityType.BOOKING, created.id()))
                .extracting("eventType")
                .containsExactly(AuditEventType.BOOKING_CREATED, AuditEventType.BOOKING_CANCELLED);
    }

    @Test
    void cancelBookingRejectsAlreadyCancelledBooking() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-cancel-twice-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-cancel-twice-customer@example.com");
        Instant startsAt = nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0));

        String createResponse = mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BookingResponse created = objectMapper.readValue(createResponse, BookingResponse.class);

        mockMvc.perform(post("/api/bookings/{bookingId}/cancel", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelBookingRequest("Cambio programma"))))
                .andExpect(status().isNoContent());

        Booking cancelled = bookings.findById(created.id()).orElseThrow();
        assertThat(cancelled.getCancellationReason()).isEqualTo("Cambio programma");

        mockMvc.perform(post("/api/bookings/{bookingId}/cancel", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_006"));
    }

    @Test
    void createBookingRejectsOverlappingSlot() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-overlap-provider@example.com");
        String firstCustomerToken = createCustomerAndLogin("booking-overlap-one@example.com");
        String secondCustomerToken = createCustomerAndLogin("booking-overlap-two@example.com");
        Instant startsAt = nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0));

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt.plusSeconds(30 * 60)
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_003"));
    }

    @Test
    void createBookingRejectsSlotOutsideAvailability() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-unavailable-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-unavailable-customer@example.com");
        Instant startsAt = nextSlot(DayOfWeek.MONDAY, LocalTime.of(13, 0));

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_003"));
    }

    @Test
    void createBookingRejectsServiceUnavailableOnThatDay() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-service-day-provider@example.com");
        OfferedService tuesdayOnlyService = offeredServices.save(new OfferedService(
                provider.provider(),
                "Controllo",
                null,
                30,
                3000
        ));
        availabilities.save(new Availability(
                provider.provider(),
                tuesdayOnlyService,
                (short) 2,
                LocalTime.of(14, 0),
                LocalTime.of(16, 0)
        ));
        String customerToken = createCustomerAndLogin("booking-service-day-customer@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                tuesdayOnlyService.getId(),
                                nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0))
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_003"));

        LocalDate tuesday = nextDate(DayOfWeek.TUESDAY);
        mockMvc.perform(get("/api/booking-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("providerId", provider.provider().getId().toString())
                        .param("serviceId", tuesdayOnlyService.getId().toString())
                        .param("from", tuesday.toString())
                        .param("to", tuesday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startsAt").value(tuesday.atTime(14, 0).toInstant(ZoneOffset.UTC).toString()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void createBookingRejectsSlotNotAlignedToGeneratedSlots() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-unaligned-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-unaligned-customer@example.com");
        Instant startsAt = nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 30));

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_003"));
    }

    @Test
    void customerCanSeeGeneratedWeeklySlotsWithBookedStatus() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-slots-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-slots-customer@example.com");
        LocalDate date = nextDate(DayOfWeek.MONDAY);
        Instant startsAt = date.atTime(10, 0).toInstant(ZoneOffset.UTC);

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/booking-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("providerId", provider.provider().getId().toString())
                        .param("serviceId", provider.service().getId().toString())
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startsAt").value(date.atTime(9, 0).toInstant(ZoneOffset.UTC).toString()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].startsAt").value(startsAt.toString()))
                .andExpect(jsonPath("$[1].status").value("BOOKED"))
                .andExpect(jsonPath("$[2].startsAt").value(date.atTime(11, 0).toInstant(ZoneOffset.UTC).toString()))
                .andExpect(jsonPath("$[2].status").value("AVAILABLE"));
    }

    @Test
    void availabilityExceptionBlocksSlotAndRejectsBooking() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-blocked-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-blocked-customer@example.com");
        LocalDate date = nextDate(DayOfWeek.MONDAY);
        Instant blockedStartsAt = date.atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant blockedEndsAt = date.atTime(11, 0).toInstant(ZoneOffset.UTC);
        availabilityExceptions.save(new AvailabilityException(
                provider.provider(),
                provider.service(),
                blockedStartsAt,
                blockedEndsAt,
                "Permesso"
        ));

        mockMvc.perform(get("/api/booking-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("providerId", provider.provider().getId().toString())
                        .param("serviceId", provider.service().getId().toString())
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].startsAt").value(blockedStartsAt.toString()))
                .andExpect(jsonPath("$[1].status").value("BLOCKED"))
                .andExpect(jsonPath("$[2].status").value("AVAILABLE"));

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                blockedStartsAt
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_003"));
    }

    @Test
    void providerWideAvailabilityExceptionBlocksEveryService() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-provider-wide-block-provider@example.com");
        OfferedService secondService = offeredServices.save(new OfferedService(provider.provider(), "Controllo", null, 60, 3000));
        availabilities.save(new Availability(provider.provider(), secondService, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        String customerToken = createCustomerAndLogin("booking-provider-wide-block-customer@example.com");
        LocalDate date = nextDate(DayOfWeek.MONDAY);
        Instant blockedStartsAt = date.atTime(9, 0).toInstant(ZoneOffset.UTC);
        availabilityExceptions.save(new AvailabilityException(
                provider.provider(),
                null,
                blockedStartsAt,
                date.atTime(10, 0).toInstant(ZoneOffset.UTC),
                "Chiusura studio"
        ));

        mockMvc.perform(get("/api/booking-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("providerId", provider.provider().getId().toString())
                        .param("serviceId", secondService.getId().toString())
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startsAt").value(blockedStartsAt.toString()))
                .andExpect(jsonPath("$[0].status").value("BLOCKED"));
    }

    @Test
    void bookingSlotsRejectsDateTimeQueryParameters() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-slots-invalid-date-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-slots-invalid-date-customer@example.com");

        mockMvc.perform(get("/api/booking-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("providerId", provider.provider().getId().toString())
                        .param("serviceId", provider.service().getId().toString())
                        .param("from", "2026-06-01T09:00:00.000Z")
                        .param("to", "2026-06-07T09:00:00.000Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.from.code").value("VAL_006"))
                .andExpect(jsonPath("$.fields.to").doesNotExist());
    }

    @Test
    void customerCannotAccessAnotherCustomerBooking() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-owner-provider@example.com");
        String ownerToken = createCustomerAndLogin("booking-owner@example.com");
        String otherToken = createCustomerAndLogin("booking-other@example.com");
        Instant startsAt = nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0));

        String createResponse = mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BookingResponse created = objectMapper.readValue(createResponse, BookingResponse.class);

        mockMvc.perform(get("/api/bookings/{bookingId}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_001"));
    }

    @Test
    void providerCanSeeAgendaForReceivedBookings() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-agenda-provider@example.com");
        String customerToken = createCustomerAndLogin("booking-agenda-customer@example.com");
        String providerToken = login(provider.user().getEmail());
        LocalDate date = nextDate(DayOfWeek.MONDAY);
        Instant startsAt = date.atTime(9, 0).toInstant(ZoneOffset.UTC);

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/providers/me/agenda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value(provider.provider().getId()))
                .andExpect(jsonPath("$[0].serviceId").value(provider.service().getId()))
                .andExpect(jsonPath("$[0].startsAt").value(startsAt.toString()));
    }

    @Test
    void providerCanConfirmRejectAndCancelReceivedBookings() throws Exception {
        ProviderFixture provider = createProviderFixture("booking-workflow-provider@example.com");
        String providerToken = login(provider.user().getEmail());
        String firstCustomerToken = createCustomerAndLogin("booking-workflow-first@example.com");
        String secondCustomerToken = createCustomerAndLogin("booking-workflow-second@example.com");
        String thirdCustomerToken = createCustomerAndLogin("booking-workflow-third@example.com");

        BookingResponse first = createBooking(firstCustomerToken, provider, nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0)));
        BookingResponse second = createBooking(secondCustomerToken, provider, nextSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0)));
        BookingResponse third = createBooking(thirdCustomerToken, provider, nextSlot(DayOfWeek.MONDAY, LocalTime.of(11, 0)));

        mockMvc.perform(get("/api/providers/me/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(first.id()));

        mockMvc.perform(get("/api/providers/me/bookings/{bookingId}", first.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/providers/me/bookings/{bookingId}/confirm", first.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/providers/me/bookings/{bookingId}/reject", second.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectBookingRequest("Slot non disponibile"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/providers/me/bookings/{bookingId}/cancel", third.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelBookingRequest("Imprevisto provider"))))
                .andExpect(status().isNoContent());

        Booking cancelled = bookings.findById(third.id()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getCancelledBy().getId()).isEqualTo(provider.user().getId());
        assertThat(cancelled.getCancellationReason()).isEqualTo("Imprevisto provider");

        assertThat(auditLogs.findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(AuditEntityType.BOOKING, first.id()))
                .extracting("eventType")
                .containsExactly(AuditEventType.BOOKING_CREATED, AuditEventType.BOOKING_CONFIRMED);
        assertThat(auditLogs.findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(AuditEntityType.BOOKING, second.id()))
                .extracting("eventType")
                .containsExactly(AuditEventType.BOOKING_CREATED, AuditEventType.BOOKING_REJECTED);
        assertThat(auditLogs.findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(AuditEntityType.BOOKING, third.id()))
                .extracting("eventType")
                .containsExactly(AuditEventType.BOOKING_CREATED, AuditEventType.BOOKING_CANCELLED);
    }

    @Test
    void providerBookingWorkflowRejectsInvalidTransitionsAndForeignBookings() throws Exception {
        ProviderFixture owner = createProviderFixture("booking-workflow-owner@example.com");
        ProviderFixture otherProvider = createProviderFixture("booking-workflow-other-provider@example.com");
        String ownerProviderToken = login(owner.user().getEmail());
        String otherProviderToken = login(otherProvider.user().getEmail());
        String customerToken = createCustomerAndLogin("booking-workflow-owner-customer@example.com");

        BookingResponse booking = createBooking(customerToken, owner, nextSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0)));

        mockMvc.perform(post("/api/providers/me/bookings/{bookingId}/confirm", booking.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherProviderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_001"));

        mockMvc.perform(post("/api/providers/me/bookings/{bookingId}/confirm", booking.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerProviderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/providers/me/bookings/{bookingId}/reject", booking.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerProviderToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_006"));
    }

    private ProviderFixture createProviderFixture(String email) {
        AppUser user = createUser(email, UserRole.PROVIDER);
        Provider provider = providers.save(new Provider(user, "Studio Booking", null, "wellness", "Milano", null));
        OfferedService service = offeredServices.save(new OfferedService(provider, "Prima visita", null, 60, 5000));
        availabilities.save(new Availability(provider, service, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        return new ProviderFixture(user, provider, service);
    }

    private String createCustomerAndLogin(String email) throws Exception {
        createUser(email, UserRole.CUSTOMER);
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

    private BookingResponse createBooking(String customerToken, ProviderFixture provider, Instant startsAt) throws Exception {
        String response = mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                provider.provider().getId(),
                                provider.service().getId(),
                                startsAt
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, BookingResponse.class);
    }

    private AppUser createUser(String email, UserRole role) {
        return users.save(new AppUser(email, passwordEncoder.encode("Password1!"), role));
    }

    private Instant nextSlot(DayOfWeek dayOfWeek, LocalTime time) {
        return nextDate(dayOfWeek).atTime(time).toInstant(ZoneOffset.UTC);
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        return LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.next(dayOfWeek));
    }

    private record ProviderFixture(AppUser user, Provider provider, OfferedService service) {
    }
}
