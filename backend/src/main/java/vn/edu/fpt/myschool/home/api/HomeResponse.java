package vn.edu.fpt.myschool.home.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.home.domain.HomeAnnouncement;
import vn.edu.fpt.myschool.home.domain.HomeDashboard;
import vn.edu.fpt.myschool.home.domain.HomeStudent;
import vn.edu.fpt.myschool.home.domain.HomeSummary;

public record HomeResponse(
        Student student,
        AcademicTermResponse academicTerm,
        Summary summary,
        List<Announcement> announcements) {

    static HomeResponse from(HomeDashboard dashboard) {
        return new HomeResponse(
                Student.from(dashboard.student()),
                AcademicTermResponse.from(dashboard.academicTerm()),
                Summary.from(dashboard.summary()),
                dashboard.announcements().stream().map(Announcement::from).toList());
    }

    public record Student(
            String studentCode,
            String fullName,
            int gradeLevel,
            String className) {

        private static Student from(HomeStudent student) {
            return new Student(
                    student.studentCode(),
                    student.fullName(),
                    student.gradeLevel(),
                    student.className());
        }
    }

    public record AcademicTermResponse(
            String academicYear,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn) {

        private static AcademicTermResponse from(AcademicTerm academicTerm) {
            if (academicTerm == null) {
                return null;
            }
            return new AcademicTermResponse(
                    academicTerm.academicYear(),
                    academicTerm.code(),
                    academicTerm.name(),
                    academicTerm.startsOn(),
                    academicTerm.endsOn());
        }
    }

    public record Summary(
            Lessons lessons,
            Events events,
            Forms forms,
            Clubs clubs) {

        private static Summary from(HomeSummary summary) {
            return new Summary(
                    new Lessons(summary.lessons().today()),
                    new Events(summary.events().upcoming()),
                    new Forms(summary.forms().pending()),
                    new Clubs(summary.clubs().active()));
        }
    }

    public record Lessons(int today) {
    }

    public record Events(int upcoming) {
    }

    public record Forms(int pending) {
    }

    public record Clubs(int active) {
    }

    public record Announcement(UUID id, String title, String body, Instant publishedAt) {

        private static Announcement from(HomeAnnouncement announcement) {
            return new Announcement(
                    announcement.id(),
                    announcement.title(),
                    announcement.body(),
                    announcement.publishedAt());
        }
    }
}
