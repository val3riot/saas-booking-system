package it.booking.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ProviderRepository providers;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void registerReturnsBearerToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("new@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse auth = objectMapper.readValue(response, AuthResponse.class);
        assertThat(auth.token()).isNotBlank();
        assertThat(jwtService.parseToken(auth.token()).role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void loginNormalizesEmail() throws Exception {
        users.save(new AppUser("normalized@example.com", passwordEncoder.encode("Password1!")));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("Normalized@Example.COM", "Password1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse auth = objectMapper.readValue(response, AuthResponse.class);
        AuthenticatedUser authenticatedUser = jwtService.parseToken(auth.token());
        assertThat(authenticatedUser.email()).isEqualTo("normalized@example.com");
        assertThat(users.findByEmail("normalized@example.com")).isPresent();
    }

    @Test
    void providerCanRegisterWithBusinessProfile() throws Exception {
        ProviderRegistrationRequest request = new ProviderRegistrationRequest(
                "provider-register@example.com",
                "Password1!",
                "Studio Provider",
                "Consulenze professionali",
                "consulting",
                "Milano",
                "Via Roma 1"
        );

        String response = mockMvc.perform(post("/api/auth/register/provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse auth = objectMapper.readValue(response, AuthResponse.class);
        AuthenticatedUser authenticatedUser = jwtService.parseToken(auth.token());
        assertThat(authenticatedUser.role()).isEqualTo(UserRole.PROVIDER);
        assertThat(providers.findByUserId(authenticatedUser.id()))
                .isPresent()
                .get()
                .extracting("businessName")
                .isEqualTo("Studio Provider");
    }

    @Test
    void loginRejectsBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("missing@example.com", "Password1!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void loginRejectsDisabledAccount() throws Exception {
        AppUser user = new AppUser("disabled@example.com", passwordEncoder.encode("Password1!"));
        user.disable();
        users.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("disabled@example.com", "Password1!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        AuthRequest request = new AuthRequest("duplicate@example.com", "Password1!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("weak@example.com", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.password.code").value("AUTH_003"))
                .andExpect(jsonPath("$.fields.password.message").value("Invalid password policy"));
    }

    @Test
    void registerRejectsUnsafePasswordCharacter() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("unsafe@example.com", "Password1'"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.password.code").value("AUTH_003"));
    }

    @Test
    void registerRejectsInvalidEmailWithSpecificFieldCode() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("not-an-email", "Password1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VAL_000"))
                .andExpect(jsonPath("$.fields.email.code").value("VAL_003"))
                .andExpect(jsonPath("$.fields.email.message").value("Invalid email format"));
    }
}
