package vn.edu.fpt.myschool.assistant.infrastructure.ai;

import org.springframework.ai.tool.annotation.Tool;

import vn.edu.fpt.myschool.assistant.application.StudentAssistantDataFacade;
import vn.edu.fpt.myschool.assistant.application.SubjectGradeFact;

public final class AuthorizedStudentTools {

    private final String authenticatedUserId;
    private final StudentAssistantDataFacade data;

    public AuthorizedStudentTools(String authenticatedUserId, StudentAssistantDataFacade data) {
        this.authenticatedUserId = authenticatedUserId;
        this.data = data;
    }

    @Tool(description = "Read the authenticated student's own profile, class, grade level, and student code.")
    public String studentProfile() {
        return data.profile(authenticatedUserId);
    }

    @Tool(description = "Read the authenticated student's current-week timetable. No student ID input is accepted.")
    public String currentTimetable() {
        return data.timetable(authenticatedUserId);
    }

    @Tool(description = "Read only today's timetable for the authenticated student.")
    public String todayTimetable() {
        return data.timetableForDay(authenticatedUserId, 0);
    }

    @Tool(description = "Read only tomorrow's timetable for the authenticated student.")
    public String tomorrowTimetable() {
        return data.timetableForDay(authenticatedUserId, 1);
    }

    @Tool(description = "Read the authenticated student's current semester grades. No student ID input is accepted.")
    public String currentGrades() {
        return data.grades(authenticatedUserId);
    }

    @Tool(description = "Return structured facts for one named subject in the authenticated student's current semester. The model must compose the user-facing answer.")
    public SubjectGradeFact currentGradeForSubject(String subjectName) {
        return data.gradeForSubject(authenticatedUserId, subjectName);
    }

    @Tool(description = "Read upcoming events visible to the authenticated student.")
    public String upcomingEvents() {
        return data.events(authenticatedUserId);
    }

    @Tool(description = "Read forms owned by the authenticated student.")
    public String studentForms() {
        return data.forms(authenticatedUserId);
    }

    @Tool(description = "Read clubs visible to the authenticated student and their membership states.")
    public String visibleClubs() {
        return data.clubs(authenticatedUserId);
    }

    @Tool(description = "Read announcements visible to the authenticated student.")
    public String visibleAnnouncements() {
        return data.announcements(authenticatedUserId);
    }
}
