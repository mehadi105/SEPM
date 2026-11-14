package com.university.ums.repo;

import com.university.ums.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByTeacherId(Long teacherId);

    @Query("""
            select c from Course c
            join fetch c.department
            left join fetch c.teacher
            order by c.code
            """)
    List<Course> findAllWithDetails();

    @Query("""
            select c from Course c
            join fetch c.department
            left join fetch c.teacher
            where c.id = :id
            """)
    Optional<Course> findWithDetailsById(@Param("id") Long id);

    @Query("""
            select c from Course c
            join fetch c.department
            left join fetch c.teacher
            where c.teacher.id = :teacherId
            order by c.code
            """)
    List<Course> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);
}
