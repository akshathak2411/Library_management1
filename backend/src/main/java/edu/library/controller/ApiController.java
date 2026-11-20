package edu.library.controller;

import edu.library.model.*;
import edu.library.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ApiController {
    private final BookRepository books; private final StudentRepository students; private final LoanRepository loans; private final LibraryUserRepository users;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    public ApiController(BookRepository b, StudentRepository s, LoanRepository l, LibraryUserRepository u){books=b;students=s;loans=l;users=u;}
    private void requireLogin(HttpSession session){if(session.getAttribute("userId")==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please log in");}
    private void requireAdmin(HttpSession session){requireLogin(session);if(!"ADMIN".equals(session.getAttribute("role"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Library admin access required");}
    @GetMapping("/books") public List<Book> getBooks(HttpSession session){requireLogin(session);return books.findAll();}
    @PostMapping("/books") public Book addBook(@RequestBody Book book,HttpSession session){requireAdmin(session);
        if(book.getTitle()==null||book.getTitle().isBlank()||book.getAuthor()==null||book.getAuthor().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Title and author are required");
        if(book.getTotalQuantity()<1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be at least 1"); book.setAvailableQuantity(book.getTotalQuantity());return books.save(book);}
    @GetMapping("/students") public List<Student> getStudents(HttpSession session){requireAdmin(session);return students.findAll();}
    @Transactional
    @PostMapping("/students") public Student addStudent(@RequestBody Map<String,String> body,HttpSession session){requireAdmin(session);
        String name=body.get("name"),email=body.get("email"),password=body.get("password");
        if(name==null||name.isBlank()||email==null||email.isBlank()||password==null||password.length()<6) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name, email and password (minimum 6 characters) are required");
        if(students.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"A student with this email address already exists");
        Student student=students.save(new Student(name,email,body.get("department")));
        student.setStudentCode("stu"+student.getId());
        student=students.save(student);
        users.save(new LibraryUser(student.getStudentCode(),encoder.encode(password),"STUDENT",student));
        return student;}
    @Transactional
    @DeleteMapping("/students/{id}") public Map<String,String> deleteStudent(@PathVariable Long id,HttpSession session){requireAdmin(session);
        Student student=students.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));
        if(loans.existsByStudent_Id(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot delete a student with loan history");
        users.findByStudent_Id(id).ifPresent(users::delete);
        students.delete(student);
        return Map.of("message","Student deleted");}
    @GetMapping("/loans") public List<Map<String,Object>> getLoans(HttpSession session){requireAdmin(session);return loans.findAll().stream().map(this::loanView).toList();}
    @GetMapping("/student/loans") public List<Map<String,Object>> studentLoans(HttpSession session){requireLogin(session);if(!"STUDENT".equals(session.getAttribute("role"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Student login required");Long sid=(Long)session.getAttribute("studentId");return loans.findAll().stream().filter(x->x.getStudent().getId().equals(sid)).map(this::loanView).toList();}
    @PostMapping("/loans/issue") public Map<String,Object> issue(@RequestBody Map<String,Long> body,HttpSession session){requireAdmin(session);
        Book book=books.findById(body.get("bookId")).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Book not found")); Student student=students.findById(body.get("studentId")).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));
        if(book.getAvailableQuantity()<1) throw new ResponseStatusException(HttpStatus.CONFLICT,"No copies available");book.setAvailableQuantity(book.getAvailableQuantity()-1);books.save(book);return loanView(loans.save(new Loan(book,student,LocalDate.now(),LocalDate.now().plusDays(14))));}
    @PostMapping("/loans/{id}/return") public Map<String,Object> returnBook(@PathVariable Long id,HttpSession session){requireAdmin(session);Loan loan=loans.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan not found"));if(loan.getReturnDate()!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Book already returned");loan.setReturnDate(LocalDate.now());Book book=loan.getBook();book.setAvailableQuantity(book.getAvailableQuantity()+1);books.save(book);return loanView(loans.save(loan));}
    @GetMapping("/reports/summary") public Map<String,Object> summary(HttpSession session){requireAdmin(session);List<Book> allBooks=books.findAll();List<Loan> allLoans=loans.findAll();long issued=allLoans.stream().filter(x->x.getReturnDate()==null).count();long overdue=allLoans.stream().filter(x->x.getReturnDate()==null&&x.getDueDate().isBefore(LocalDate.now())).count();return Map.of("totalBooks",allBooks.stream().mapToInt(Book::getTotalQuantity).sum(),"availableBooks",allBooks.stream().mapToInt(Book::getAvailableQuantity).sum(),"issuedBooks",issued,"overdueBooks",overdue,"students",students.count());}
    // Kept available to the local Python analysis script; protect this endpoint before any public deployment.
    @GetMapping("/reports/reading-patterns") public List<Map<String,Object>> readingPatterns(){return loans.findAll().stream().map(this::loanView).toList();}
    private Map<String,Object> loanView(Loan x){Map<String,Object> m=new LinkedHashMap<>();m.put("id",x.getId());m.put("bookId",x.getBook().getId());m.put("bookTitle",x.getBook().getTitle());m.put("category",x.getBook().getCategory());m.put("studentId",x.getStudent().getId());m.put("studentName",x.getStudent().getName());m.put("issueDate",x.getIssueDate());m.put("dueDate",x.getDueDate());m.put("returnDate",x.getReturnDate());m.put("status",x.getReturnDate()!=null?"RETURNED":(x.getDueDate().isBefore(LocalDate.now())?"OVERDUE":"ISSUED"));return m;}
}
