package edu.library.controller;

import edu.library.model.LibraryUser;
import edu.library.repository.LibraryUserRepository;
import edu.library.repository.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AuthController {
    private final LibraryUserRepository users;
    private final StudentRepository students;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public AuthController(LibraryUserRepository users, StudentRepository students){this.users=users;this.students=students;}

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body, HttpSession session){
        String username=body.getOrDefault("username", "").trim();
        String password=body.getOrDefault("password", "");
        String requestedRole=body.getOrDefault("role", "").toUpperCase();
        LibraryUser user;
        if("STUDENT".equals(requestedRole)) {
            Optional<LibraryUser> studentAccount=students.findByStudentCodeIgnoreCase(username)
                    .flatMap(student -> users.findByStudent_Id(student.getId()));
            if(studentAccount.isEmpty()) {
                try { studentAccount=users.findByStudent_Id(Long.parseLong(username)); }
                catch(NumberFormatException ignored) {}
            }
            user=studentAccount.or(() -> users.findByUsername(username)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid student ID or password"));
        } else {
            user=users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid username or password"));
        }
        if(!encoder.matches(password,user.getPasswordHash()) || !user.getRole().equals(requestedRole))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid username, password or login type");
        String loginId=user.getStudent()==null?user.getUsername():user.getStudent().getStudentCode();
        if(loginId==null && user.getStudent()!=null) loginId="stu"+user.getStudent().getId();
        session.setAttribute("userId",user.getId());
        session.setAttribute("username",loginId);
        session.setAttribute("role",user.getRole());
        session.setAttribute("studentId",user.getStudent()==null?null:user.getStudent().getId());
        session.setAttribute("studentName",user.getStudent()==null?null:user.getStudent().getName());
        Map<String,Object> result=new LinkedHashMap<>(); result.put("username",loginId); result.put("role",user.getRole());
        if(user.getStudent()!=null) result.put("studentName",user.getStudent().getName());
        return result;
    }
    @GetMapping("/me") public Map<String,Object> me(HttpSession session){
        if(session.getAttribute("userId")==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please log in");
        Map<String,Object> result=new LinkedHashMap<>(); result.put("username",session.getAttribute("username")); result.put("role",session.getAttribute("role")); result.put("studentId",session.getAttribute("studentId")); result.put("studentName",session.getAttribute("studentName")); return result;
    }
    @PostMapping("/logout") public Map<String,String> logout(HttpSession session){session.invalidate();return Map.of("message","Logged out");}
}
