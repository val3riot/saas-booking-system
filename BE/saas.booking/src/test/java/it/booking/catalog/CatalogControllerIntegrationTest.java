package it.booking.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.auth.AuthRequest;
import it.booking.auth.AuthResponse;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
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
        OfferedService activeService = offeredServices.save(new OfferedService(
                activeProvider,
                "Consulenza",
                "Sessione iniziale",
                60,
                5000
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

        mockMvc.perform(get("/api/catalog/providers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activeProvider.getId()))
                .andExpect(jsonPath("$[0].businessName").value("Active Studio"))
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

        mockMvc.perform(get("/api/catalog/providers/{providerId}/services", activeProvider.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activeService.getId()))
                .andExpect(jsonPath("$[0].name").value("Consulenza"))
                .andExpect(jsonPath("$[0].active").doesNotExist());

        mockMvc.perform(get("/api/catalog/providers/{providerId}/services/{serviceId}", activeProvider.getId(), inactiveService.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERV_001"));
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
        AppUser user = createUser(email, UserRole.PROVIDER);
        Provider provider = new Provider(user, businessName, null, "consulting", "Milano", "Via Roma 1");
        if (!active) {
            provider.deactivate();
        }
        return providers.save(provider);
    }

    private AppUser createUser(String email, UserRole role) {
        return users.save(new AppUser(email, passwordEncoder.encode("Password1!"), role));
    }
}
