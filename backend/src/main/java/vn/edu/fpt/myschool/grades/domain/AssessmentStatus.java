package vn.edu.fpt.myschool.grades.domain;

public enum AssessmentStatus {
    RECORDED,
    MAKE_UP_REQUIRED,
    EXCUSED,
    ABSENT_FINALIZED;

    public boolean isFinalized() {
        return this == RECORDED || this == ABSENT_FINALIZED;
    }
}
