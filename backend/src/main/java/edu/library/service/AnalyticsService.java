package edu.library.service;

import edu.library.model.Loan;
import edu.library.model.Student;
import edu.library.repository.LoanRepository;
import edu.library.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final LoanRepository loans;
    private final StudentRepository students;

    public AnalyticsService(LoanRepository loans, StudentRepository students) {
        this.loans = loans;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> booksPerStudent(LocalDate startDate, LocalDate endDate, String department) {
        Map<Long, Long> counts = countByStudent(filteredLoans(startDate, endDate, department));
        return students.findAll().stream()
                .filter(student -> matchesDepartment(student, department))
                .map(student -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studentId", student.getId());
                    row.put("studentName", student.getName());
                    row.put("department", departmentName(student));
                    row.put("totalBorrowed", counts.getOrDefault(student.getId(), 0L));
                    return row;
                })
                .sorted(Comparator.comparingLong((Map<String, Object> row) -> (Long) row.get("totalBorrowed"))
                        .reversed().thenComparing(row -> (String) row.get("studentName"), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> departmentReading(LocalDate startDate, LocalDate endDate, String department) {
        Map<String, Long> counts = filteredLoans(startDate, endDate, department).stream()
                .collect(Collectors.groupingBy(loan -> departmentName(loan.getStudent()),
                        TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> Map.<String, Object>of("department", entry.getKey(), "totalBorrowed", entry.getValue()))
                .sorted(Comparator.comparingLong((Map<String, Object> row) -> (Long) row.get("totalBorrowed")).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> monthlyTrend(LocalDate startDate, LocalDate endDate, String department) {
        List<Loan> matchingLoans = filteredLoans(startDate, endDate, department);
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(11);
        Map<YearMonth, Long> counts = matchingLoans.stream()
                .filter(loan -> {
                    YearMonth month = YearMonth.from(loan.getIssueDate());
                    return !month.isBefore(firstMonth) && !month.isAfter(currentMonth);
                })
                .collect(Collectors.groupingBy(loan -> YearMonth.from(loan.getIssueDate()), Collectors.counting()));
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth month = firstMonth.plusMonths(i);
            result.add(Map.of(
                    "month", month.toString(),
                    "label", month.format(labelFormat),
                    "totalBorrowed", counts.getOrDefault(month, 0L)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topCategories(LocalDate startDate, LocalDate endDate, String department) {
        return filteredLoans(startDate, endDate, department).stream()
                .collect(Collectors.groupingBy(loan -> {
                    String category = loan.getBook().getCategory();
                    return category == null || category.isBlank() ? "Uncategorized" : category.trim();
                }, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("category", entry.getKey(), "totalBorrowed", entry.getValue()))
                .sorted(Comparator.comparingLong((Map<String, Object> row) -> (Long) row.get("totalBorrowed"))
                        .reversed().thenComparing(row -> (String) row.get("category"), String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topReaders(LocalDate startDate, LocalDate endDate, String department) {
        Map<Long, Long> counts = countByStudent(filteredLoans(startDate, endDate, department));
        return students.findAll().stream()
                .filter(student -> matchesDepartment(student, department))
                .filter(student -> counts.containsKey(student.getId()))
                .sorted(Comparator.comparingLong((Student student) -> counts.get(student.getId())).reversed()
                        .thenComparing(Student::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(student -> Map.<String, Object>of(
                        "studentId", student.getId(),
                        "studentName", student.getName(),
                        "department", departmentName(student),
                        "totalBorrowed", counts.get(student.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(LocalDate startDate, LocalDate endDate, String department) {
        List<Student> matchingStudents = students.findAll().stream()
                .filter(student -> matchesDepartment(student, department))
                .toList();
        List<Loan> matchingLoans = filteredLoans(startDate, endDate, department);
        YearMonth currentMonth = YearMonth.now();
        long activeReaders = matchingLoans.stream()
                .filter(loan -> YearMonth.from(loan.getIssueDate()).equals(currentMonth))
                .map(loan -> loan.getStudent().getId())
                .distinct()
                .count();
        long totalBorrowed = matchingLoans.size();
        double average = matchingStudents.isEmpty() ? 0 : (double) totalBorrowed / matchingStudents.size();
        return Map.of(
                "totalStudents", matchingStudents.size(),
                "totalBorrowed", totalBorrowed,
                "activeReadersThisMonth", activeReaders,
                "averageBooksPerStudent", Math.round(average * 100.0) / 100.0);
    }

    private List<Loan> filteredLoans(LocalDate startDate, LocalDate endDate, String department) {
        return loans.findAll().stream()
                .filter(loan -> loan.getIssueDate() != null)
                .filter(loan -> startDate == null || !loan.getIssueDate().isBefore(startDate))
                .filter(loan -> endDate == null || !loan.getIssueDate().isAfter(endDate))
                .filter(loan -> matchesDepartment(loan.getStudent(), department))
                .toList();
    }

    private Map<Long, Long> countByStudent(List<Loan> matchingLoans) {
        return matchingLoans.stream().map(loan -> loan.getStudent().getId())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private boolean matchesDepartment(Student student, String department) {
        return department == null || department.isBlank()
                || departmentName(student).equalsIgnoreCase(department.trim());
    }

    private String departmentName(Student student) {
        return student.getDepartment() == null || student.getDepartment().isBlank()
                ? "Unspecified" : student.getDepartment().trim();
    }
}
