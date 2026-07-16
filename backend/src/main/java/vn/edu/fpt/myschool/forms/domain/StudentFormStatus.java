package vn.edu.fpt.myschool.forms.domain;

public enum StudentFormStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean canBeCancelledByStudent() {
        return this == SUBMITTED || this == IN_REVIEW;
    }
}
