package edu.library.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Book book;
    @ManyToOne(optional=false) private Student student;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public Loan() {}
    public Loan(Book book, Student student, LocalDate issueDate, LocalDate dueDate) {
        this.book=book; this.student=student; this.issueDate=issueDate; this.dueDate=dueDate;
    }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Book getBook(){return book;} public void setBook(Book b){this.book=b;}
    public Student getStudent(){return student;} public void setStudent(Student s){this.student=s;}
    public LocalDate getIssueDate(){return issueDate;} public void setIssueDate(LocalDate d){this.issueDate=d;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate d){this.dueDate=d;}
    public LocalDate getReturnDate(){return returnDate;} public void setReturnDate(LocalDate d){this.returnDate=d;}
}
