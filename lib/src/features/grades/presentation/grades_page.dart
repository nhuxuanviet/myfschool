import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../app/student_app_bar.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/grades_providers.dart';
import '../domain/semester_grades.dart';

class GradesPage extends ConsumerWidget {
  const GradesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final grades = ref.watch(semesterGradesProvider);
    return Semantics(
      label: 'Điểm học kỳ',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.surface,
        appBar: studentAppBar(context: context, title: 'Kết quả học tập'),
        body: SafeArea(
          child: grades.when(
            loading: () => Center(
              child: Semantics(
                label: 'Đang tải điểm học kỳ',
                liveRegion: true,
                child: const CircularProgressIndicator(),
              ),
            ),
            error: (_, _) => _LoadError(
              onRetry: () => ref.invalidate(semesterGradesProvider),
            ),
            data: (value) => _GradesContent(grades: value),
          ),
        ),
      ),
    );
  }
}

class _GradesContent extends ConsumerWidget {
  const _GradesContent({required this.grades});

  final SemesterGrades grades;

  static const colors = [
    AppColors.primary,
    Color(0xFF2582F3),
    Color(0xFF16A36A),
    Color(0xFF7A4AF6),
    Color(0xFFB8BCC5),
    Color(0xFF12B7A7),
    Color(0xFFF3A40D),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final term = grades.selectedTerm;
    return RefreshIndicator(
      onRefresh: () => ref.refresh(semesterGradesProvider.future),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(
            maxWidth: AppDimensions.contentMaxWidth,
          ),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(24, 30, 24, 28),
            children: [
              Row(
                children: [
                  Expanded(
                    child: Semantics(
                      label: 'Chọn học kỳ ${term.name}, ${term.academicYear}',
                      button: true,
                      onTap: () => _pickTerm(context, ref),
                      excludeSemantics: true,
                      child: InkWell(
                        borderRadius: BorderRadius.circular(12),
                        onTap: () => _pickTerm(context, ref),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Kết quả ${term.name.toLowerCase()}',
                              style: Theme.of(context).textTheme.headlineMedium,
                            ),
                            const SizedBox(height: 10),
                            Text(
                              term.academicYear.replaceAll('-', ' – '),
                              style: const TextStyle(
                                color: AppColors.mutedText,
                                fontSize: 18,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  IconButton(
                    tooltip: 'Chia sẻ kết quả',
                    onPressed: () => _shareGrades(context),
                    icon: const Icon(Icons.ios_share_outlined, size: 27),
                  ),
                ],
              ),
              const SizedBox(height: 26),
              const Divider(height: 1),
              if (grades.subjects.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 48),
                  child: Text(
                    'Chưa có kết quả môn học trong học kỳ này.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: AppColors.mutedText),
                  ),
                )
              else
                for (var index = 0; index < grades.subjects.length; index++)
                  _SubjectRow(
                    subject: grades.subjects[index],
                    termId: term.id,
                    accent: colors[index % colors.length],
                  ),
              const SizedBox(height: 30),
              const Text(
                'Điểm số được cập nhật khi giáo viên công bố',
                textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.mutedText, fontSize: 13),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _shareGrades(BuildContext context) async {
    final summary = grades.subjects
        .map((subject) => '${subject.name}: ${_shortResult(subject)}')
        .join('\n');
    await Clipboard.setData(
      ClipboardData(
        text:
            '${grades.selectedTerm.name} - '
            '${grades.selectedTerm.academicYear}\n$summary',
      ),
    );
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Đã sao chép kết quả học kỳ.')),
    );
  }

  Future<void> _pickTerm(BuildContext context, WidgetRef ref) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Chọn học kỳ',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 12),
              for (final term in grades.availableTerms)
                Semantics(
                  label: 'Chọn ${term.name}, ${term.academicYear}',
                  button: true,
                  child: ListTile(
                    selected: term.id == grades.selectedTerm.id,
                    selectedColor: AppColors.primary,
                    title: Text(term.name),
                    subtitle: Text(term.academicYear),
                    trailing: term.id == grades.selectedTerm.id
                        ? const Icon(Icons.check)
                        : null,
                    onTap: () => Navigator.pop(sheetContext, term.id),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
    if (selected != null) {
      ref.read(selectedGradeTermIdProvider.notifier).select(selected);
    }
  }
}

class _SubjectRow extends StatelessWidget {
  const _SubjectRow({
    required this.subject,
    required this.termId,
    required this.accent,
  });

  final GradeSubject subject;
  final String termId;
  final Color accent;

  @override
  Widget build(BuildContext context) {
    final summary = _subjectSummary(subject);
    return Semantics(
      label: 'Môn ${subject.name}. $summary',
      container: true,
      explicitChildNodes: true,
      child: Semantics(
        label: 'Xem chi tiết ${subject.name}',
        button: true,
        excludeSemantics: true,
        child: InkWell(
          onTap: () => context.goNamed(
            AppRouteNames.gradeDetails,
            pathParameters: {'subjectCode': subject.code},
            queryParameters: {'termId': termId},
          ),
          child: Container(
            height: 74,
            decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppColors.border)),
            ),
            child: Row(
              children: [
                Container(
                  width: 3,
                  height: 34,
                  decoration: BoxDecoration(
                    color: accent,
                    borderRadius: BorderRadius.circular(99),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Text(
                    subject.name,
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                Text(
                  _shortResult(subject),
                  style: TextStyle(
                    color: _hasResult(subject)
                        ? AppColors.text
                        : AppColors.mutedText,
                    fontSize: _hasResult(subject) ? 23 : 16,
                    fontWeight: _hasResult(subject)
                        ? FontWeight.w700
                        : FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _LoadError extends StatelessWidget {
  const _LoadError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Semantics(
        label: 'Không thể tải điểm học kỳ',
        child: FilledButton.icon(
          onPressed: onRetry,
          icon: const Icon(Icons.refresh),
          label: const Text('Thử lại'),
        ),
      ),
    );
  }
}

bool _hasResult(GradeSubject subject) =>
    subject.termAverage != null || subject.termResult != null;

String _shortResult(GradeSubject subject) {
  if (subject.termAverage case final average?) return _formatScore(average);
  if (subject.termResult case final result?) return result.label;
  return 'Chưa có điểm';
}

String _subjectSummary(GradeSubject subject) {
  if (subject.termAverage case final average?) {
    return 'Điểm trung bình học kỳ: ${_formatScore(average)}';
  }
  if (subject.termResult case final result?) {
    return 'Kết quả học kỳ: ${result.label}';
  }
  return 'Chờ cập nhật điểm học kỳ';
}

String _formatScore(double value) {
  final text = value.toStringAsFixed(1);
  return text.endsWith('.0') ? text.substring(0, text.length - 2) : text;
}

String formatGradeScore(double value) =>
    _formatScore(value).replaceAll('.', ',');
