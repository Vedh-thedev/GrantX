package com.grantx.repository;

import com.grantx.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    
    @Query("SELECT s FROM Student s WHERE s.studentId = :registerNumber")
    Optional<Student> findByRegisterNumber(@Param("registerNumber") String registerNumber);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE s.studentId = :registerNumber")
    boolean existsByRegisterNumber(@Param("registerNumber") String registerNumber);

    Optional<Student> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
}
