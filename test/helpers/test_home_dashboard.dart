import 'package:myfschoolse1913/src/features/home/domain/home_dashboard.dart';

HomeDashboard testHomeDashboard({
  String fullName = 'Nguyễn Văn A',
  String className = '10A1',
  int gradeLevel = 10,
  List<HomeAnnouncement>? announcements,
}) {
  return HomeDashboard(
    student: HomeStudent(
      studentCode: 'HS001',
      fullName: fullName,
      gradeLevel: gradeLevel,
      className: className,
    ),
    academicTerm: AcademicTerm(
      academicYear: '2025–2026',
      code: 'SEMESTER_1',
      name: 'Học kỳ I',
      startsOn: DateTime(2025, 9, 5),
      endsOn: DateTime(2026, 1, 17),
    ),
    summary: const HomeSummary(
      todayLessons: 5,
      upcomingEvents: 2,
      pendingForms: 1,
      activeClubs: 1,
    ),
    announcements:
        announcements ??
        [
          HomeAnnouncement(
            id: 'announcement-1',
            title: 'Chào mừng năm học mới',
            body: 'Nhà trường chào đón học sinh trở lại.',
            publishedAt: DateTime(2025, 9, 1, 8),
          ),
        ],
  );
}
