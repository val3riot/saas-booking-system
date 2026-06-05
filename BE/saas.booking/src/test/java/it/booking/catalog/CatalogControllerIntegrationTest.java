package it.booking.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.auth.AuthRequest;
import it.booking.auth.AuthResponse;
import it.booking.availability.Availability;
import it.booking.availability.AvailabilityRepository;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Test
    void catalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/catalog/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void customerCanBrowseOnlyActiveProvidersAndServices() throws Exception {
        String token = createCustomerAndLogin("catalog-customer@example.com");
        Provider activeProvider = createProvider("catalog-active-provider@example.com", "Active Studio", true);
        Provider inactiveProvider = createProvider("catalog-inactive-provider@example.com", "Inactive Studio", false);
        Provider disabledAccountProvider = createProvider("catalog-disabled-account-provider@example.com", "Disabled Account Studio", true);
        disabledAccountProvider.getUser().disable();
        users.save(disabledAccountProvider.getUser());
        OfferedService activeService = offeredServices.save(new OfferedService(
                activeProvider,
                "Consulenza",
                "Sessione iniziale",
                60,
                5000
        ));
        availabilities.save(new Availability(activeProvider, activeService, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        OfferedService serviceWithoutAvailability = offeredServices.save(new OfferedService(
                activeProvider,
                "Attivo senza disponibilita",
                null,
                30,
                3000
        ));
        OfferedService inactiveService = offeredServices.save(new OfferedService(
                activeProvider,
                "Servizio nascosto",
                null,
                30,
                2000
        ));
        inactiveService.deactivate();
        offeredServices.save(inactiveService);
        offeredServices.save(new OfferedService(inactiveProvider, "Non visibile", null, 30, 2000));
        OfferedService disabledAccountService = offeredServices.save(new OfferedService(disabledAccountProvider, "Non prenotabile", null, 30, 2000));
        availabilities.save(new Availability(disabledAccountProvider, disabledAccountService, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0)));

        mockMvc.perform(get("/api/catalog/providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(activeProvider.getId().intValue())))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(disabledAccountProvider.getId().intValue()))))
                .andExpect(jsonPath("$[*].businessName").value(hasItem("Active Studio")))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());

        mockMvc.perform(get("/api/catalog/providers/{providerId}", activeProvider.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Active Studio"));

        mockMvc.perform(get("/api/catalog/providers/{providerId}", inactiveProvider.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROV_001"));

        mockMvc.perform(get("/api/catalog/providers/{providerId}", disabledAccountProvider.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROV_001"));

        mockMvc.perform(get("/api/catalog/providers/{providerId}/services", activeProvider.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activeService.getId()))
                .andExpect(jsonPath("$[0].name").value("Consulenza"))
                .andExpect(jsonPath("$[1]").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());

        mockMvc.perform(get("/api/catalog/providers/{providerId}/services/{serviceId}", activeProvider.getId(), serviceWithoutAvailability.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERV_001"));

        mockMvc.perform(get("/api/catalog/providers/{providerId}/services/{serviceId}", activeProvider.getId(), inactiveService.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERV_001"));
    }

    @Test
    void customerCanSearchProvidersWithFiltersAndPagination() throws Exception {
        String token = createCustomerAndLogin("catalog-search-customer@example.com");
        Provider milanConsulting = createProvider(
                "catalog-search-milan-consulting@example.com",
                "Step Five Alpha Consulting",
                "consulting",
                "Milano",
                true
        );
        Provider romeWellness = createProvider(
                "catalog-search-rome-wellness@example.com",
                "Step Five Beta Wellness",
                "wellness",
                "Roma",
                true
        );
        createProvider(
                "catalog-search-hidden@example.com",
                "Gamma Consulting",
                "consulting",
                "Milano",
                false
        );

        OfferedService service = offeredServices.save(new OfferedService(
                milanConsulting,
                "Consulenza strategica",
                null,
                60,
                8000
        ));
        availabilities.save(new Availability(
                milanConsulting,
                service,
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        ));
        offeredServices.save(new OfferedService(romeWellness, "Massaggio", null, 60, 7000));

        mockMvc.perform(get("/api/catalog/providers/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("query", "step five alpha")
                        .param("category", "consulting")
                        .param("city", "milano")
                        .param("availableOn", "2026-06-08")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "BUSINESS_NAME")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(milanConsulting.getId()))
                .andExpect(jsonPath("$.content[0].businessName").value("Step Five Alpha Consulting"))
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(get("/api/catalog/providers/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("query", "step five beta")
                        .param("category", "wellness")
                        .param("city", "roma")
                        .param("availableOn", "2026-06-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void customerCanSearchProvidersWithoutOptionalFilters() throws Exception {
        String token = createCustomerAndLogin("catalog-search-no-filters-customer@example.com");
        Provider bariProvider = createProvider(
                "catalog-search-bari@example.com",
                "Step Five Bari Studio",
                "medical",
                "Bari",
                true
        );
        Provider torinoProvider = createProvider(
                "catalog-search-torino@example.com",
                "Step Five Torino Studio",
                "consulting",
                "Torino",
                true
        );
        createProvider(
                "catalog-search-inactive-no-filters@example.com",
                "Step Five Hidden Studio",
                "consulting",
                "Torino",
                false
        );
        OfferedService bariService = offeredServices.save(new OfferedService(bariProvider, "Visita Bari", null, 60, 5000));
        availabilities.save(new Availability(bariProvider, bariService, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        OfferedService torinoService = offeredServices.save(new OfferedService(torinoProvider, "Visita Torino", null, 60, 5000));
        availabilities.save(new Availability(torinoProvider, torinoService, (short) 2, LocalTime.of(9, 0), LocalTime.of(12, 0)));

        mockMvc.perform(get("/api/catalog/providers/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("query", "step five")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "CITY")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bariProvider.getId()))
                .andExpect(jsonPath("$.content[0].city").value("Bari"))
                .andExpect(jsonPath("$.content[1].id").value(torinoProvider.getId()))
                .andExpect(jsonPath("$.content[1].city").value("Torino"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void searchProvidersRejectsInvalidPagination() throws Exception {
        String token = createCustomerAndLogin("catalog-search-validation@example.com");

        mockMvc.perform(get("/api/catalog/providers/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.page.code").value("VAL_002"))
                .andExpect(jsonPath("$.fields.size.code").value("VAL_002"));
    }

    private String createCustomerAndLogin(String email) throws Exception {
        createUser(email, UserRole.CUSTOMER);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "Password1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private Provider createProvider(String email, String businessName, boolean active) {
        return createProvider(email, businessName, "consulting", "Milano", active);
    }

    private Provider createProvider(String email, String businessName, String category, String city, boolean active) {
        AppUser user = createUser(email, UserRole.PROVIDER);
        Provider provider = new Provider(user, businessName, null, category, city, "Via Roma 1");
        if (!active) {
            provider.deactivate();
        }
        return providers.save(provider);
    }

    private AppUser createUser(String email, UserRole role) {
        return users.save(new AppUser(email, passwordEncoder.encode("Password1!"), role));
    }
}
