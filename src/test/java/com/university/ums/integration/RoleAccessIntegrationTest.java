package com.university.ums.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleAccessIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rolesAreEnforcedForAdminTeacherAndStudent() throws Exception {
        String admin = token("admin", "admin123");
        String teacher = token("teacher", "teacher123");
        String student = token("student", "student123");
        String james = token("teacher2", "teacher123");

        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Physics\",\"code\":\"PHY\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"code\":\"EE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        mvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Electrical Engineering\",\"code\":\"ee\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("EE"));

        mvc.perform(post("/api/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CS301","title":"Databases","credits":3,"departmentId":1}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/courses/teaching").header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("CS101", "CS201")));

        mvc.perform(get("/api/enrollments").header(HttpHeaders.AUTHORIZATION, bearer(james)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].course.code", hasItem("MATH101")))
                .andExpect(jsonPath("$[*].course.code", not(hasItem("CS101"))));

        mvc.perform(get("/api/courses/teaching").header(HttpHeaders.AUTHORIZATION, bearer(student)))
                .andExpect(status().isForbidden());

        MvcResult me = mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andReturn();
        Number adminId = JsonPath.read(me.getResponse().getContentAsString(), "$.id");
        mvc.perform(delete("/api/users/" + adminId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete your own account"));
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
