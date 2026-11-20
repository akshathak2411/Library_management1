package edu.library.repository;
import edu.library.model.LibraryUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LibraryUserRepository extends JpaRepository<LibraryUser,Long> {
    Optional<LibraryUser> findByUsername(String username);
    Optional<LibraryUser> findByStudent_Id(Long studentId);
    boolean existsByUsername(String username);
}
