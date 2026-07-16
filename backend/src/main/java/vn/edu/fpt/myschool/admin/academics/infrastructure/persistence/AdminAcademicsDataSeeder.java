package vn.edu.fpt.myschool.admin.academics.infrastructure.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("(dev | e2e) & !prod")
@Order(250)
class AdminAcademicsDataSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    AdminAcademicsDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        jdbcTemplate.update("""
                WITH latest_year AS (
                    SELECT id
                    FROM academic_years
                    ORDER BY ends_on DESC, id
                    LIMIT 1
                ), student_classes AS (
                    SELECT DISTINCT student.class_name,
                                    student.grade_level,
                                    academic_year.id AS academic_year_id
                    FROM students student
                    CROSS JOIN latest_year academic_year
                )
                INSERT INTO school_classes (
                    id, academic_year_id, code, name, grade_level,
                    enabled, version, created_at, updated_at
                )
                SELECT
                    md5(student_class.academic_year_id::text || ':' || student_class.class_name)::uuid,
                    student_class.academic_year_id,
                    student_class.class_name,
                    student_class.class_name,
                    student_class.grade_level,
                    TRUE,
                    0,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                FROM student_classes student_class
                ON CONFLICT (academic_year_id, code) DO NOTHING
                """);
        jdbcTemplate.update("""
                UPDATE students student
                SET class_id = school_class.id
                FROM school_classes school_class
                WHERE student.class_name = school_class.code
                  AND student.grade_level = school_class.grade_level
                  AND student.class_id IS NULL
                """);
    }
}
