package com.university.ums.unit;

import com.university.ums.domain.Department;
import com.university.ums.dto.DepartmentRequest;
import com.university.ums.dto.DepartmentResponse;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.DepartmentRepository;
import com.university.ums.service.DepartmentService;
import com.university.ums.service.DtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private CourseRepository courseRepository;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository, courseRepository, new DtoMapper());
    }

    @Test
    void createNormalizesCodeBeforeSaving() {
        when(departmentRepository.findByCode("EE")).thenReturn(Optional.empty());
        when(departmentRepository.findByNameIgnoreCase("Electrical Engineering")).thenReturn(Optional.empty());
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(7L);
            return department;
        });

        DepartmentResponse response = departmentService.create(
                new DepartmentRequest("Electrical Engineering", "ee"));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        assertEquals("EE", captor.getValue().getCode());
        assertEquals("EE", response.code());
        assertEquals(7L, response.id());
    }
}
