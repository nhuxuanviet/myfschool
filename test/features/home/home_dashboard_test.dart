import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/home/domain/home_dashboard.dart';

void main() {
  test('parses the authenticated homepage dashboard contract', () {
    final dashboard = HomeDashboard.fromJson(_dashboardJson());

    expect(dashboard.student.fullName, 'Nguyễn Văn A');
    expect(dashboard.student.gradeLevel, 10);
    expect(dashboard.academicTerm?.code, 'SEMESTER_1');
    expect(dashboard.summary.todayLessons, 5);
    expect(dashboard.announcements.single.title, 'Chào mừng năm học mới');
  });

  test('rejects non-integer dashboard counters', () {
    final json = _dashboardJson();
    json['summary'] = {
      'lessons': {'today': '5'},
      'events': {'upcoming': 2},
      'forms': {'pending': 1},
      'clubs': {'active': 1},
    };

    expect(() => HomeDashboard.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects an invalid academic-term date', () {
    final json = _dashboardJson();
    (json['academicTerm'] as Map<String, dynamic>)['startsOn'] = '09/05/2025';

    expect(() => HomeDashboard.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('accepts a dashboard while no academic term is active', () {
    final json = _dashboardJson()..['academicTerm'] = null;

    final dashboard = HomeDashboard.fromJson(json);

    expect(dashboard.academicTerm, isNull);
  });
}

Map<String, dynamic> _dashboardJson() => {
  'student': {
    'studentCode': 'HS001',
    'fullName': 'Nguyễn Văn A',
    'gradeLevel': 10,
    'className': '10A1',
  },
  'academicTerm': {
    'academicYear': '2025–2026',
    'code': 'SEMESTER_1',
    'name': 'Học kỳ I',
    'startsOn': '2025-09-05',
    'endsOn': '2026-01-17',
  },
  'summary': {
    'lessons': {'today': 5},
    'events': {'upcoming': 2},
    'forms': {'pending': 1},
    'clubs': {'active': 1},
  },
  'announcements': [
    {
      'id': 'announcement-1',
      'title': 'Chào mừng năm học mới',
      'body': 'Nhà trường chào đón học sinh trở lại.',
      'publishedAt': '2025-09-01T08:00:00Z',
    },
  ],
};
