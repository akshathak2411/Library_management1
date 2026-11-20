package edu.library.config;

import edu.library.model.*;
import edu.library.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDate;
import java.util.Optional;

@Configuration
public class DataLoader {
    @Bean CommandLineRunner seed(BookRepository books, StudentRepository students, LoanRepository loans, LibraryUserRepository users) {
        return args -> {
            BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
            Optional<LibraryUser> admin=users.findByUsername("Admin");
            if(admin.isEmpty()) {
                admin=users.findByUsername("admin");
                if(admin.isPresent()) {
                    LibraryUser existingAdmin=admin.get();
                    existingAdmin.setUsername("Admin");
                }
            }
            if(admin.isPresent()) {
                LibraryUser existingAdmin=admin.get();
                if(!encoder.matches("admin@123",existingAdmin.getPasswordHash())) {
                    existingAdmin.setPasswordHash(encoder.encode("admin@123"));
                }
                users.save(existingAdmin);
            } else {
                users.save(new LibraryUser("Admin",encoder.encode("admin@123"),"ADMIN",null));
            }
            if(books.count()==0) {
                Book b1=books.save(new Book("Wings of Fire","A.P.J. Abdul Kalam","Biography",5));
                Book b2=books.save(new Book("The Alchemist","Paulo Coelho","Fiction",4));
                Book b3=books.save(new Book("Clean Code","Robert C. Martin","Technology",3));
                Student s1=students.save(new Student("Ananya","ananya@college.edu","IT"));
                Student s2=students.save(new Student("Arun","arun@college.edu","CSE"));
                loans.save(new Loan(b1,s1,LocalDate.now().minusDays(5),LocalDate.now().plusDays(9)));
                loans.save(new Loan(b2,s2,LocalDate.now().minusDays(20),LocalDate.now().minusDays(6)));
                b1.setAvailableQuantity(4); b2.setAvailableQuantity(3); books.save(b1); books.save(b2);
            }
            for(Student student:students.findAll()) {
                if(student.getStudentCode()==null || student.getStudentCode().isBlank()) {
                    student.setStudentCode("stu"+student.getId());
                    students.save(student);
                }
            }
            for(String email:new String[]{"ananya@college.edu","arun@college.edu"}) {
                Student student=students.findAll().stream()
                        .filter(x->x.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
                if(student!=null && users.findByStudent_Id(student.getId()).isEmpty()
                        && !users.existsByUsername(student.getStudentCode())) {
                    users.save(new LibraryUser(student.getStudentCode(),encoder.encode("Student@123"),"STUDENT",student));
                }
            }
        };
    }
}
