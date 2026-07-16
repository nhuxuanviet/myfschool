import 'package:myfschoolse1913/src/features/timetable/domain/timetable.dart';

Timetable testTimetable({
  String weekStart = '2026-07-13',
  List<Map<String, dynamic>>? academicTerms,
  Map<int, List<Map<String, dynamic>>>? lessonsByDay,
}) {
  return Timetable.fromJson(
    testTimetableJson(
      weekStart: weekStart,
      academicTerms: academicTerms,
      lessonsByDay: lessonsByDay,
    ),
  );
}

Map<String, dynamic> testTimetableJson({
  String weekStart = '2026-07-13',
  List<Map<String, dynamic>>? academicTerms,
  Map<int, List<Map<String, dynamic>>>? lessonsByDay,
}) {
  final start = _parseDate(weekStart);
  return {
    'timeZone': 'Asia/Ho_Chi_Minh',
    'weekStart': weekStart,
    'weekEnd': _formatDate(start.add(const Duration(days: 6))),
    'academicTerms': academicTerms ?? [testAcademicTermJson()],
    'days': List.generate(7, (index) {
      return {
        'date': _formatDate(start.add(Duration(days: index))),
        'dayOfWeek': TimetableDayOfWeek.values[index].apiValue,
        'lessons': lessonsByDay?[index] ?? _defaultLessons(index),
      };
    }),
  };
}

Map<String, dynamic> testAcademicTermJson({
  String academicYear = '2026-2027',
  String code = 'SEMESTER_1',
  String name = 'Học kỳ I',
  String startsOn = '2026-07-13',
  String endsOn = '2026-12-31',
}) {
  return {
    'academicYear': academicYear,
    'code': code,
    'name': name,
    'startsOn': startsOn,
    'endsOn': endsOn,
  };
}

Map<int, List<Map<String, dynamic>>> testEmptyLessonsByDay() {
  return {
    for (var index = 0; index < 7; index += 1) index: <Map<String, dynamic>>[],
  };
}

List<Map<String, dynamic>> _defaultLessons(int dayIndex) {
  return switch (dayIndex) {
    0 => [
      _lesson(
        periodNumber: 1,
        startTime: '07:00',
        endTime: '07:45',
        subjectCode: 'MATH',
        subjectName: 'Toán học',
        teacherName: 'Cô Lan',
        room: 'A101',
      ),
      _lesson(
        periodNumber: 2,
        startTime: '07:55',
        endTime: '08:40',
        subjectCode: 'LIT',
        subjectName: 'Ngữ văn',
        teacherName: 'Thầy Minh',
        room: 'A101',
      ),
    ],
    2 => [
      _lesson(
        periodNumber: 1,
        startTime: '07:00',
        endTime: '07:45',
        subjectCode: 'HIS',
        subjectName: 'Lịch sử',
        teacherName: 'Cô Hoa',
        room: 'A102',
      ),
      _lesson(
        periodNumber: 2,
        startTime: '07:55',
        endTime: '08:40',
        subjectCode: 'CHEM',
        subjectName: 'Hóa học',
        teacherName: 'Cô Mai',
        room: 'B204',
        status: 'CANCELLED',
        note: 'Nghỉ để tham gia hoạt động toàn trường',
      ),
    ],
    4 => [
      _lesson(
        periodNumber: 1,
        startTime: '13:00',
        endTime: '13:45',
        session: 'AFTERNOON',
        subjectCode: 'PHY',
        subjectName: 'Vật lý',
        teacherName: 'Thầy Nam',
        room: 'B301',
        status: 'REPLACED',
        note: 'Đổi giáo viên giảng dạy',
      ),
    ],
    5 => [
      _lesson(
        periodNumber: 1,
        startTime: '07:00',
        endTime: '07:45',
        subjectCode: 'ENG',
        subjectName: 'Tiếng Anh',
        teacherName: 'Cô Hương',
        room: 'A203',
        status: 'ADDED',
        note: 'Học bù tiết đã nghỉ',
      ),
    ],
    _ => const [],
  };
}

Map<String, dynamic> _lesson({
  required int periodNumber,
  required String startTime,
  required String endTime,
  required String subjectCode,
  required String subjectName,
  required String? teacherName,
  required String? room,
  String session = 'MORNING',
  String status = 'SCHEDULED',
  String? note,
}) {
  return {
    'session': session,
    'periodNumber': periodNumber,
    'startTime': startTime,
    'endTime': endTime,
    'subject': {'code': subjectCode, 'name': subjectName},
    'teacherName': teacherName,
    'room': room,
    'status': status,
    'note': note,
  };
}

DateTime _parseDate(String value) {
  final parts = value.split('-').map(int.parse).toList(growable: false);
  return DateTime.utc(parts[0], parts[1], parts[2]);
}

String _formatDate(DateTime value) {
  String twoDigits(int number) => number.toString().padLeft(2, '0');
  return '${value.year.toString().padLeft(4, '0')}-${twoDigits(value.month)}-'
      '${twoDigits(value.day)}';
}
