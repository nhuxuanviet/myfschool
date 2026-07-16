package vn.edu.fpt.myschool.assistant.application;

interface StudentAssistantData {
    String profile(String authenticatedUserId);

    String timetable(String authenticatedUserId);

    String timetableForDay(String authenticatedUserId, int dayOffset);

    String grades(String authenticatedUserId);

    SubjectGradeFact gradeForSubject(String authenticatedUserId, String subjectName);

    String events(String authenticatedUserId);

    String forms(String authenticatedUserId);

    String clubs(String authenticatedUserId);

    String announcements(String authenticatedUserId);
}
