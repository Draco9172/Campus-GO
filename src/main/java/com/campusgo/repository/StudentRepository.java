package com.campusgo.repository;

import com.campusgo.entity.Student;
import com.campusgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByUser(User user);

    Optional<Student> findByUserId(Long userId);

    boolean existsByUsn(String usn);
}
