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

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void studentEnrollsAndOnlyCourseTeacherCanGrade() throws Exception {
        String maya = token("student3", "student123");
        String sarah = token("teacher", "teacher123");
        String james = token("teacher2", "teacher123");
        int cs201 = courseId(maya, "CS201");
        int bus110 = courseId(maya, "BUS110");

        MvcResult enrolled = mvc.perform(post("/api/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(maya))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + cs201 + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ENROLLED"))
                .andExpect(jsonPath("$.student.username").value("student3"))
                .andReturn();
        Number enrollmentId = JsonPath.read(enrolled.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(maya))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + cs201 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already enrolled in this course"));

        mvc.perform(post("/api/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(sarah))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + cs201 + "}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/enrollments/" + enrollmentId + "/grade")
                        .header(HttpHeaders.AUTHORIZATION, bearer(james))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"A\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only record grades for your own courses"));

        mvc.perform(patch("/api/enrollments/" + enrollmentId + "/grade")
                        .header(HttpHeaders.AUTHORIZATION, bearer(maya))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"A\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/enrollments/" + enrollmentId + "/grade")
                        .header(HttpHeaders.AUTHORIZATION, bearer(sarah))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("A"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(delete("/api/enrollments/" + enrollmentId).header(HttpHeaders.AUTHORIZATION, bearer(maya)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/enrollments").header(HttpHeaders.AUTHORIZATION, bearer(maya)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].grade", hasItem("A")));

        MvcResult dropped = mvc.perform(post("/api/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(maya))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + bus110 + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number openEnrollment = JsonPath.read(dropped.getResponse().getContentAsString(), "$.id");
        mvc.perform(delete("/api/enrollments/" + openEnrollment).header(HttpHeaders.AUTHORIZATION, bearer(maya)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/enrollments").header(HttpHeaders.AUTHORIZATION, bearer(maya)))
                .andExpect(jsonPath("$[*].course.code", not(hasItem("BUS110"))));
    }

    private int courseId(String token, String code) throws Exception {
        MvcResult result = mvc.perform(get("/api/courses").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> ids = JsonPath.read(result.getResponse().getContentAsString(), "$[?(@.code == '" + code + "')].id");
        return ids.get(0).intValue();
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
