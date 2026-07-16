package vn.edu.fpt.myschool.assistant.application;

import org.springframework.stereotype.Component;

@Component
public class StudentAssistantDataFacade {

    private final StudentAssistantData delegate;

    StudentAssistantDataFacade(StudentAssistantData delegate) {
        this.delegate = delegate;
    }

    public String profile(String subject) {
        return delegate.profile(subject);
    }

    public String timetable(String subject) {
        return delegate.timetable(subject);
    }

    public String timetableForDay(String subject, int dayOffset) {
        return delegate.timetableForDay(subject, dayOffset);
    }

    public String grades(String subject) {
        return delegate.grades(subject);
    }

    public SubjectGradeFact gradeForSubject(String subject, String subjectName) {
        return delegate.gradeForSubject(subject, subjectName);
    }

    public String events(String subject) {
        return delegate.events(subject);
    }

    public String forms(String subject) {
        return delegate.forms(subject);
    }

    public String clubs(String subject) {
        return delegate.clubs(subject);
    }

    public String announcements(String subject) {
        return delegate.announcements(subject);
    }
}
