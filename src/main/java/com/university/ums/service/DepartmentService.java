package com.university.ums.service;

import com.university.ums.domain.Department;
import com.university.ums.dto.DepartmentRequest;
import com.university.ums.dto.DepartmentResponse;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final DtoMapper mapper;

    public List<DepartmentResponse> list() {
        return departmentRepository.findAllByOrderByNameAsc().stream().map(mapper::toDepartment).toList();
    }

    public DepartmentResponse get(Long id) {
        return mapper.toDepartment(require(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        ensureUnique(code, name, null);
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        return mapper.toDepartment(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = require(id);
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        ensureUnique(code, name, id);
        department.setCode(code);
        department.setName(name);
        return mapper.toDepartment(department);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (courseRepository.existsByDepartmentId(id)) {
            throw ApiException.conflict("Delete this department's courses before deleting the department");
        }
        departmentRepository.deleteById(id);
    }

    private Department require(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
    }

    private void ensureUnique(String code, String name, Long currentId) {
        departmentRepository.findByCode(code)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Department code is already in use");
                });
        departmentRepository.findByNameIgnoreCase(name)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Department name is already in use");
                });
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
