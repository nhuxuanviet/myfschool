import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../features/auth/data/auth_network_providers.dart';

class ChildSummary {
  const ChildSummary({
    required this.studentId,
    required this.studentCode,
    required this.fullName,
    required this.gradeLevel,
    required this.className,
    required this.relationship,
  });

  factory ChildSummary.fromJson(Map<String, dynamic> json) {
    return ChildSummary(
      studentId: json['studentId'] as String,
      studentCode: json['studentCode'] as String? ?? '',
      fullName: json['fullName'] as String? ?? '',
      gradeLevel: (json['gradeLevel'] as num?)?.toInt() ?? 0,
      className: json['className'] as String? ?? '',
      relationship: json['relationship'] as String? ?? 'GUARDIAN',
    );
  }

  final String studentId;
  final String studentCode;
  final String fullName;
  final int gradeLevel;
  final String className;
  final String relationship;

  String get relationshipLabel => switch (relationship) {
    'FATHER' => 'Bố',
    'MOTHER' => 'Mẹ',
    _ => 'Người giám hộ',
  };
}

/// Reads a guardian's own children. Every call names a child, and the server
/// checks the link again each time — the id here is a request, not a grant.
class ParentApi {
  const ParentApi(this._dio);

  final Dio _dio;

  Future<List<ChildSummary>> getChildren() async {
    final response = await _dio.get<List<dynamic>>('/api/v1/parent/children');
    return (response.data ?? const [])
        .map((item) => ChildSummary.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<Map<String, dynamic>> getChildGrades(String studentId) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/api/v1/parent/children/$studentId/grades',
    );
    return response.data ?? const {};
  }

  Future<Map<String, dynamic>> getChildTimetable(String studentId) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/api/v1/parent/children/$studentId/timetable',
    );
    return response.data ?? const {};
  }
}

final parentApiProvider = Provider<ParentApi>((ref) {
  return ParentApi(ref.watch(authenticatedDioProvider));
});

final parentChildrenProvider = FutureProvider<List<ChildSummary>>((ref) {
  return ref.watch(parentApiProvider).getChildren();
});

/// Which child the guardian is looking at. Null until they pick one, which
/// means "the first child" rather than "no child".
class SelectedChildId extends Notifier<String?> {
  @override
  String? build() => null;

  void select(String studentId) => state = studentId;
}

final selectedChildIdProvider =
    NotifierProvider<SelectedChildId, String?>(SelectedChildId.new);

/// The selected child, defaulting to the first one the guardian has.
final selectedChildProvider = Provider<ChildSummary?>((ref) {
  final children = ref.watch(parentChildrenProvider).value;
  if (children == null || children.isEmpty) return null;
  final selectedId = ref.watch(selectedChildIdProvider);
  if (selectedId == null) return children.first;
  return children.firstWhere(
    (child) => child.studentId == selectedId,
    orElse: () => children.first,
  );
});

final childGradesProvider = FutureProvider.family<Map<String, dynamic>, String>(
  (ref, studentId) => ref.watch(parentApiProvider).getChildGrades(studentId),
);

final childTimetableProvider =
    FutureProvider.family<Map<String, dynamic>, String>(
      (ref, studentId) =>
          ref.watch(parentApiProvider).getChildTimetable(studentId),
    );
