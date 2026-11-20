package edu.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "library_users")
public class LibraryUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String username;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private String role;
    @OneToOne private Student student;
    public LibraryUser() {}
    public LibraryUser(String username, String passwordHash, String role, Student student) {
        this.username=username; this.passwordHash=passwordHash; this.role=role; this.student=student;
    }
    public Long getId(){return id;} public String getUsername(){return username;}
    public void setUsername(String username){this.username=username;}
    public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
    public String getPasswordHash(){return passwordHash;} public String getRole(){return role;}
    public Student getStudent(){return student;}
}
