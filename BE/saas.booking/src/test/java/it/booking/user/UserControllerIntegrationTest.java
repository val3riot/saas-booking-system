package it.booking.user;

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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void userEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void userEndpointsRequireAdminRole() throws Exception {
        String customerToken = registerAndGetToken("customer-forbidden@example.com");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void adminCanManageUsers() throws Exception {
        String adminToken = createAdminAndLogin("admin-users@example.com");
        CreateUserRequest createRequest = new CreateUserRequest(
                "provider-user@example.com",
                "Password1!",
                UserRole.PROVIDER
        );

        String createResponse = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("provider-user@example.com"))
                .andExpect(jsonPath("$.role").value("PROVIDER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponse created = objectMapper.readValue(createResponse, UserResponse.class);

        mockMvc.perform(get("/api/users/{id}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));

        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "updated-provider@example.com",
                UserRole.CUSTOMER,
                true
        );

        mockMvc.perform(put("/api/users/{id}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated-provider@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        mockMvc.perform(post("/api/users/{id}/disable", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        AppUser disabled = users.findById(created.id()).orElseThrow();
        assertThat(disabled.isEnabled()).isFalse();

        mockMvc.perform(post("/api/users/{id}/enable", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        AppUser enabled = users.findById(created.id()).orElseThrow();
        assertThat(enabled.isEnabled()).isTrue();

        mockMvc.perform(delete("/api/users/{id}", created.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        AppUser deleted = users.findById(created.id()).orElseThrow();
        assertThat(deleted.isEnabled()).isFalse();
    }

    @Test
    void adminCreateUserRejectsDuplicateEmail() throws Exception {
        String adminToken = createAdminAndLogin("admin-duplicate@example.com");
        users.save(new AppUser("existing-user@example.com", passwordEncoder.encode("Password1!"), UserRole.CUSTOMER));
        CreateUserRequest request = new CreateUserRequest("existing-user@example.com", "Password1!", UserRole.PROVIDER);

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_005"));
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
        users.save(new AppUser(email, passwordEncoder.encode("Password1!"), UserRole.ADMIN));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "Password1!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }
}
