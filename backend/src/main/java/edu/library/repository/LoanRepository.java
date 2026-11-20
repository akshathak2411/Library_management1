package edu.library.repository;
import edu.library.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LoanRepository extends JpaRepository<Loan, Long> {
    boolean existsByStudent_Id(Long studentId);
}
