package it.booking.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Repository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        ApiExceptionHandlerIntegrationTest.DataIntegrityFixtureController.class,
        ApiExceptionHandlerIntegrationTest.DuplicateUserFixture.class
})
class ApiExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private DuplicateUserFixture duplicateUserFixture;

    @Test
    @WithMockUser(roles = "ADMIN")
    void dataIntegrityViolationOnEmailUniqueConstraintReturnsConflict() throws Exception {
        users.save(new AppUser("db-conflict@example.com", "Password1!", UserRole.CUSTOMER));
        duplicateUserFixture.setEmail("db-conflict@example.com");

        mockMvc.perform(post("/test/data-integrity/email")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @RestController
    @RequestMapping("/test/data-integrity")
    static class DataIntegrityFixtureController {

        private final DuplicateUserFixture duplicateUserFixture;

        DataIntegrityFixtureController(DuplicateUserFixture duplicateUserFixture) {
            this.duplicateUserFixture = duplicateUserFixture;
        }

        @PostMapping("/email")
        void duplicateEmail() {
            duplicateUserFixture.insertDuplicateEmail();
        }
    }

    @Repository
    static class DuplicateUserFixture {

        private final EntityManager entityManager;
        private String email;

        DuplicateUserFixture(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        void setEmail(String email) {
            this.email = email;
        }

        @Transactional
        void insertDuplicateEmail() {
            try {
                entityManager.createNativeQuery("""
                                insert into app_users (email, password_hash, role, enabled, created_at, updated_at)
                                values (:email, 'hash', 'CUSTOMER', true, current_timestamp, current_timestamp)
                                """)
                        .setParameter("email", email)
                        .executeUpdate();
                entityManager.flush();
            } catch (RuntimeException ex) {
                throw new DataIntegrityViolationException("uq_app_users_email", ex);
            }
        }
    }
}
