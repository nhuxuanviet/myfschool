import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../features/auth/data/auth_network_providers.dart';

/// A class-subject the signed-in teacher is responsible for this term.
class AssignedClass {
  const AssignedClass({
    required this.classId,
    required this.classCode,
    required this.subjectId,
    required this.subjectName,
    required this.academicTermId,
    required this.studentCount,
    required this.homeroom,
  });

  factory AssignedClass.fromJson(Map<String, dynamic> json) {
    return AssignedClass(
      classId: json['classId'] as String,
      classCode: json['classCode'] as String? ?? '',
      subjectId: json['subjectId'] as String,
      subjectName: json['subjectName'] as String? ?? '',
      academicTermId: json['academicTermId'] as String,
      studentCount: (json['studentCount'] as num?)?.toInt() ?? 0,
      homeroom: json['homeroom'] as bool? ?? false,
    );
  }

  final String classId;
  final String classCode;
  final String subjectId;
  final String subjectName;
  final String academicTermId;
  final int studentCount;
  final bool homeroom;
}

class TeachingLesson {
  const TeachingLesson({
    required this.lessonDate,
    required this.session,
    required this.periodNumber,
    required this.startTime,
    required this.endTime,
    required this.classCode,
    required this.subjectName,
    required this.room,
    required this.status,
  });

  factory TeachingLesson.fromJson(Map<String, dynamic> json) {
    return TeachingLesson(
      lessonDate: DateTime.parse(json['lessonDate'] as String),
      session: json['session'] as String? ?? '',
      periodNumber: (json['periodNumber'] as num?)?.toInt() ?? 0,
      startTime: json['startTime'] as String? ?? '',
      endTime: json['endTime'] as String? ?? '',
      classCode: json['classCode'] as String? ?? '',
      subjectName: json['subjectName'] as String? ?? '',
      room: json['room'] as String?,
      status: json['status'] as String? ?? 'SCHEDULED',
    );
  }

  final DateTime lessonDate;
  final String session;
  final int periodNumber;
  final String startTime;
  final String endTime;
  final String classCode;
  final String subjectName;
  final String? room;
  final String status;

  bool get isCancelled => status == 'CANCELLED';
}

class TeachingWeek {
  const TeachingWeek({required this.weekStart, required this.lessons});

  factory TeachingWeek.fromJson(Map<String, dynamic> json) {
    final lessons = (json['lessons'] as List<dynamic>? ?? const [])
        .map((item) => TeachingLesson.fromJson(item as Map<String, dynamic>))
        .toList();
    return TeachingWeek(
      weekStart: DateTime.parse(json['weekStart'] as String),
      lessons: lessons,
    );
  }

  final DateTime weekStart;
  final List<TeachingLesson> lessons;
}

class HomeroomClass {
  const HomeroomClass({
    required this.classId,
    required this.classCode,
    required this.studentCount,
  });

  factory HomeroomClass.fromJson(Map<String, dynamic> json) {
    return HomeroomClass(
      classId: json['classId'] as String,
      classCode: json['classCode'] as String? ?? '',
      studentCount: (json['studentCount'] as num?)?.toInt() ?? 0,
    );
  }

  final String classId;
  final String classCode;
  final int studentCount;
}

class HomeroomLeaveForm {
  const HomeroomLeaveForm({
    required this.id,
    required this.studentFullName,
    required this.studentCode,
    required this.reason,
    required this.status,
    required this.submittedByRole,
    this.startsOn,
    this.endsOn,
  });

  factory HomeroomLeaveForm.fromJson(Map<String, dynamic> json) {
    return HomeroomLeaveForm(
      id: json['id'] as String,
      studentFullName: json['studentFullName'] as String? ?? '',
      studentCode: json['studentCode'] as String? ?? '',
      reason: json['reason'] as String? ?? '',
      status: json['status'] as String? ?? '',
      submittedByRole: json['submittedByRole'] as String? ?? 'STUDENT',
      startsOn: json['startsOn'] == null
          ? null
          : DateTime.parse(json['startsOn'] as String),
      endsOn: json['endsOn'] == null
          ? null
          : DateTime.parse(json['endsOn'] as String),
    );
  }

  final String id;
  final String studentFullName;
  final String studentCode;
  final String reason;
  final String status;
  final String submittedByRole;
  final DateTime? startsOn;
  final DateTime? endsOn;

  bool get isOpen => status == 'SUBMITTED' || status == 'IN_REVIEW';
}

class ClassStudent {
  const ClassStudent({
    required this.studentId,
    required this.studentCode,
    required this.fullName,
  });

  factory ClassStudent.fromJson(Map<String, dynamic> json) {
    return ClassStudent(
      studentId: json['studentId'] as String,
      studentCode: json['studentCode'] as String? ?? '',
      fullName: json['fullName'] as String? ?? '',
    );
  }

  final String studentId;
  final String studentCode;
  final String fullName;
}

/// Reads the teacher's own workload. Every endpoint is already scoped to the
/// signed-in teacher on the server, so nothing here passes a teacher id.
class TeacherApi {
  const TeacherApi(this._dio);

  final Dio _dio;

  Future<List<AssignedClass>> getClasses() async {
    final response = await _dio.get<List<dynamic>>('/api/v1/teacher/classes');
    return (response.data ?? const [])
        .map((item) => AssignedClass.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<HomeroomClass>> getHomerooms() async {
    final response = await _dio.get<List<dynamic>>('/api/v1/teacher/homerooms');
    return (response.data ?? const [])
        .map((item) => HomeroomClass.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<TeachingWeek> getSchedule({DateTime? weekStart}) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/api/v1/teacher/schedule',
      queryParameters: weekStart == null
          ? null
          : {'weekStart': _isoDate(weekStart)},
    );
    return TeachingWeek.fromJson(response.data ?? const {});
  }

  Future<List<ClassStudent>> getClassStudents(String classId) async {
    final response = await _dio.get<List<dynamic>>(
      '/api/v1/teacher/classes/$classId/students',
    );
    return (response.data ?? const [])
        .map((item) => ClassStudent.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<HomeroomLeaveForm>> getHomeroomForms({String? status}) async {
    final response = await _dio.get<List<dynamic>>(
      '/api/v1/teacher/homeroom-forms',
      queryParameters: status == null ? null : {'status': status},
    );
    return (response.data ?? const [])
        .map((item) => HomeroomLeaveForm.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<void> decideForm(String formId, {required bool approve, String? note}) {
    final action = approve ? 'approve' : 'reject';
    return _dio.post<void>(
      '/api/v1/teacher/homeroom-forms/$formId/$action',
      data: {if (note != null && note.isNotEmpty) 'note': note},
    );
  }

  static String _isoDate(DateTime value) {
    return '${value.year.toString().padLeft(4, '0')}-'
        '${value.month.toString().padLeft(2, '0')}-'
        '${value.day.toString().padLeft(2, '0')}';
  }
}

final teacherApiProvider = Provider<TeacherApi>((ref) {
  return TeacherApi(ref.watch(authenticatedDioProvider));
});

final teacherClassesProvider = FutureProvider<List<AssignedClass>>((ref) {
  return ref.watch(teacherApiProvider).getClasses();
});

final teacherHomeroomsProvider = FutureProvider<List<HomeroomClass>>((ref) {
  return ref.watch(teacherApiProvider).getHomerooms();
});

final teacherScheduleProvider =
    FutureProvider.family<TeachingWeek, DateTime?>((ref, weekStart) {
      return ref.watch(teacherApiProvider).getSchedule(weekStart: weekStart);
    });

final homeroomFormsProvider = FutureProvider<List<HomeroomLeaveForm>>((ref) {
  return ref.watch(teacherApiProvider).getHomeroomForms();
});

final classStudentsProvider =
    FutureProvider.family<List<ClassStudent>, String>((ref, classId) {
      return ref.watch(teacherApiProvider).getClassStudents(classId);
    });
