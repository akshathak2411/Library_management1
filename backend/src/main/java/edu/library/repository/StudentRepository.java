package edu.library.repository;
import edu.library.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByEmailIgnoreCase(String email);
    java.util.Optional<Student> findByStudentCodeIgnoreCase(String studentCode);
}
