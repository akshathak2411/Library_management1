package edu.library.controller;

import edu.library.service.AnalyticsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/books-per-student")
    public List<Map<String, Object>> booksPerStudent(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department,
            HttpSession session) {
        requireAdmin(session);
        validateDates(startDate, endDate);
        return analytics.booksPerStudent(startDate, endDate, department);
    }

    @GetMapping("/department-reading")
    public List<Map<String, Object>> departmentReading(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department,
            HttpSession session) {
        requireAdmin(session);
        validateDates(startDate, endDate);
        return analytics.departmentReading(startDate, endDate, department);
    }

    @GetMapping("/monthly-trend")
    public List<Map<String, Object>> monthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department,
            HttpSession session) {
        requireAdmin(session);
        validateDates(startDate, endDate);
        return analytics.monthlyTrend(startDate, endDate, department);
    }

    @GetMapping("/top-categories")
    public List<Map<String, Object>> topCategories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department,
            HttpSession session) {
        requireAdmin(session);
        validateDates(startDate, endDate);
        return analytics.topCategories(startDate, endDate, department);
    }

    @GetMapping("/top-readers")
    public List<Map<String, Object>> topReaders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department,
            HttpSession session) {
        requireAdmin(session);
        validateDates(startDate, endDate);
        return analytics.topReaders(startDate, endDate, department);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department,
            HttpSession session) {
        requireAdmin(session);
        validateDates(startDate, endDate);
        return analytics.summary(startDate, endDate, department);
    }

    private void requireAdmin(HttpSession session) {
        if (session.getAttribute("userId") == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        if (!"ADMIN".equals(session.getAttribute("role")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Library admin access required");
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must not be after end date");
    }
}
