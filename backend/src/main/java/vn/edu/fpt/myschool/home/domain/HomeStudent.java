package vn.edu.fpt.myschool.home.domain;

public record HomeStudent(
        String studentCode,
        String fullName,
        int gradeLevel,
        String className) {

    public HomeStudent {
        requireText(studentCode, "studentCode");
        requireText(fullName, "fullName");
        if (gradeLevel < 6 || gradeLevel > 12) {
            throw new IllegalArgumentException("gradeLevel must be between 6 and 12");
        }
        requireText(className, "className");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
