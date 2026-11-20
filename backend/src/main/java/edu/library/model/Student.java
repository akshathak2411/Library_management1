package edu.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique=true) private String studentCode;
    @Column(nullable=false) private String name;
    @Column(nullable=false, unique=true) private String email;
    private String department;

    public Student() {}
    public Student(String name,String email,String department){this.name=name;this.email=email;this.department=department;}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getStudentCode(){return studentCode;} public void setStudentCode(String code){this.studentCode=code;}
    public String getName(){return name;} public void setName(String n){this.name=n;}
    public String getEmail(){return email;} public void setEmail(String e){this.email=e;}
    public String getDepartment(){return department;} public void setDepartment(String d){this.department=d;}
}
