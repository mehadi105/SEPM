package com.university.ums.repo;

import com.university.ums.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentId(Long studentId);

    boolean existsByCourseId(Long courseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Enrollment e where e.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Enrollment e where e.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select e from Enrollment e
            join fetch e.student
            join fetch e.course c
            join fetch c.department
            left join fetch c.teacher
            order by e.enrolledAt desc
            """)
    List<Enrollment> findAllWithDetails();

    @Query("""
            select e from Enrollment e
            join fetch e.student
            join fetch e.course c
            join fetch c.department
            left join fetch c.teacher
            where e.student.id = :studentId
            order by e.enrolledAt desc
            """)
    List<Enrollment> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("""
            select e from Enrollment e
            join fetch e.student
            join fetch e.course c
            join fetch c.department
            left join fetch c.teacher
            where c.teacher.id = :teacherId
            order by c.code, e.student.fullName
            """)
    List<Enrollment> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    @Query("""
            select e from Enrollment e
            join fetch e.student
            join fetch e.course c
            join fetch c.department
            left join fetch c.teacher
            where e.id = :id
            """)
    Optional<Enrollment> findWithDetailsById(@Param("id") Long id);
}
