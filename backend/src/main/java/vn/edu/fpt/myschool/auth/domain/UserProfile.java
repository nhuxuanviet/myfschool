package vn.edu.fpt.myschool.auth.domain;

public sealed interface UserProfile
        permits StudentProfile, TeacherProfile, ParentProfile, AdminProfile {
}
