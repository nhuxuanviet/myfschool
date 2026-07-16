import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/home/data/home_api_repository.dart';

void main() {
  test('loads the dashboard from the authenticated home endpoint', () async {
    String? requestedPath;
    final dio = Dio()
      ..interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            requestedPath = options.path;
            handler.resolve(
              Response<Object?>(
                requestOptions: options,
                statusCode: 200,
                data: _dashboardJson(),
              ),
            );
          },
        ),
      );

    final dashboard = await ApiHomeRepository(ApiClient(dio)).getDashboard();

    expect(requestedPath, ApiHomeRepository.dashboardPath);
    expect(dashboard.student.studentCode, 'HS001');
    expect(dashboard.announcements, hasLength(1));
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
