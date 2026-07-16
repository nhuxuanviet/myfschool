import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/grades_providers.dart';
import '../domain/semester_grades.dart';
import 'grades_page.dart' show formatGradeScore;

class GradeDetailsPage extends ConsumerWidget {
  const GradeDetailsPage({
    required this.subjectCode,
    required this.termId,
    super.key,
  });

  final String subjectCode;
  final String termId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final grades = ref.watch(semesterGradesForTermProvider(termId));

    return grades.when(
      loading: () => const _GradeDetailsLoading(),
      error: (_, _) => _GradeDetailsError(
        onRetry: () => ref.invalidate(semesterGradesForTermProvider(termId)),
      ),
      data: (value) {
        final subject = _findSubject(value.subjects, subjectCode);
        if (subject == null) return const _MissingSubjectPage();
        return _GradeDetailsScaffold(subject: subject, termId: termId);
      },
    );
  }
}

class _GradeDetailsLoading extends StatelessWidget {
  const _GradeDetailsLoading();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      body: Center(
        child: Semantics(
          label: 'Đang tải chi tiết điểm',
          liveRegion: true,
          child: CircularProgressIndicator(color: AppColors.primary),
        ),
      ),
    );
  }
}

class _GradeDetailsError extends StatelessWidget {
  const _GradeDetailsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      appBar: AppBar(
        title: const Text('Chi tiết điểm'),
        leading: _BackToGradesButton(),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(AppDimensions.pageHorizontalPadding),
          child: Semantics(
            label: 'Không thể tải chi tiết điểm',
            container: true,
            explicitChildNodes: true,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(
                  Icons.cloud_off_outlined,
                  color: AppColors.mutedText,
                  size: 48,
                ),
                const SizedBox(height: 16),
                const Text(
                  'Không thể tải chi tiết điểm. Vui lòng thử lại.',
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Thử lại'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _MissingSubjectPage extends StatelessWidget {
  const _MissingSubjectPage();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      appBar: AppBar(
        title: const Text('Chi tiết điểm'),
        leading: _BackToGradesButton(),
      ),
      body: Center(
        child: Semantics(
          label: 'Không tìm thấy môn học',
          container: true,
          child: Text('Không tìm thấy môn học trong học kỳ đã chọn.'),
        ),
      ),
    );
  }
}

class _GradeDetailsScaffold extends ConsumerWidget {
  const _GradeDetailsScaffold({required this.subject, required this.termId});

  final GradeSubject subject;
  final String termId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Semantics(
      label: 'Chi tiết điểm ${subject.name}',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: AppBar(
          title: const ExcludeSemantics(child: Text('Chi tiết điểm')),
          leading: const _BackToGradesButton(),
        ),
        body: RefreshIndicator(
          color: AppColors.primary,
          onRefresh: () =>
              ref.refresh(semesterGradesForTermProvider(termId).future),
          child: LayoutBuilder(
            builder: (context, constraints) {
              final horizontalPadding = constraints.maxWidth >= 600
                  ? 32.0
                  : AppDimensions.pageHorizontalPadding;
              return SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: EdgeInsets.fromLTRB(
                  horizontalPadding,
                  20,
                  horizontalPadding,
                  32,
                ),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxWidth: AppDimensions.wideContentMaxWidth,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _SubjectDetailsHeader(subject: subject),
                        const SizedBox(height: 24),
                        for (final kind in AssessmentKind.values) ...[
                          _AssessmentGroup(
                            kind: kind,
                            assessments: subject.assessments
                                .where((assessment) => assessment.kind == kind)
                                .toList(growable: false),
                          ),
                          const SizedBox(height: 24),
                        ],
                      ],
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _BackToGradesButton extends StatelessWidget {
  const _BackToGradesButton();

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: 'Quay lại điểm học kỳ',
      onPressed: () => context.go(AppRoutes.grades),
      icon: const Icon(Icons.arrow_back),
    );
  }
}

class _SubjectDetailsHeader extends StatelessWidget {
  const _SubjectDetailsHeader({required this.subject});

  final GradeSubject subject;

  @override
  Widget build(BuildContext context) {
    final result = _subjectResult(subject);
    return Semantics(
      label: 'Môn ${subject.name}. $result',
      container: true,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              subject.name,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            if (subject.termAverage case final average?)
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      'Điểm trung bình học kỳ',
                      style: TextStyle(
                        color: AppColors.mutedText,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  Text(
                    formatGradeScore(average),
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontSize: 30,
                      height: 1,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              )
            else
              Text(result, style: const TextStyle(color: AppColors.mutedText)),
            const SizedBox(height: 8),
            Text(
              'Cần ${subject.requiredRegularAssessments} đánh giá thường xuyên',
              style: const TextStyle(color: AppColors.mutedText, fontSize: 13),
            ),
          ],
        ),
      ),
    );
  }
}

class _AssessmentGroup extends StatelessWidget {
  const _AssessmentGroup({required this.kind, required this.assessments});

  final AssessmentKind kind;
  final List<GradeAssessment> assessments;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Semantics(
          label: kind.sectionLabel,
          container: true,
          header: true,
          excludeSemantics: true,
          child: Text(
            kind.sectionLabel,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
          ),
        ),
        const SizedBox(height: 8),
        if (assessments.isEmpty)
          const _EmptyAssessmentGroup()
        else
          for (final assessment in assessments) ...[
            _AssessmentCard(assessment: assessment),
            const SizedBox(height: 8),
          ],
      ],
    );
  }
}

class _EmptyAssessmentGroup extends StatelessWidget {
  const _EmptyAssessmentGroup();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(14),
      ),
      child: const Text(
        'Chưa có đánh giá.',
        style: TextStyle(color: AppColors.mutedText),
      ),
    );
  }
}

class _AssessmentCard extends StatelessWidget {
  const _AssessmentCard({required this.assessment});

  final GradeAssessment assessment;

  @override
  Widget build(BuildContext context) {
    final style = _assessmentStatusStyle(assessment.status);
    final semanticLabel = [
      assessment.label,
      'Hình thức: ${assessment.form.label}',
      _assessmentResult(assessment),
      assessment.status.label,
      if (assessment.durationMinutes case final minutes?)
        'Thời lượng: $minutes phút',
      if (assessment.assessedOn case final date?)
        'Ngày đánh giá ${formatGradeDate(date)}',
    ].join('. ');
    return Semantics(
      label: semanticLabel,
      container: true,
      excludeSemantics: true,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.border),
          boxShadow: const [
            BoxShadow(
              color: Color(0x08000000),
              blurRadius: 8,
              offset: Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    assessment.label,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                _AssessmentStatusBadge(
                  label: assessment.status.label,
                  style: style,
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: Wrap(
                    spacing: 14,
                    runSpacing: 6,
                    children: [
                      _AssessmentMetadata(
                        icon: Icons.category_outlined,
                        text: 'Hình thức: ${assessment.form.label}',
                      ),
                      if (assessment.durationMinutes case final minutes?)
                        _AssessmentMetadata(
                          icon: Icons.schedule_outlined,
                          text: 'Thời lượng: $minutes phút',
                        ),
                      if (assessment.assessedOn case final date?)
                        _AssessmentMetadata(
                          icon: Icons.calendar_today_outlined,
                          text: _readableDate(date),
                        ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                if (assessment.score case final score?)
                  Text(
                    formatGradeScore(score),
                    textAlign: TextAlign.right,
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontSize: 22,
                      height: 1,
                      fontWeight: FontWeight.w800,
                    ),
                  )
                else
                  Text(
                    _assessmentResult(assessment),
                    textAlign: TextAlign.right,
                    style: const TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AssessmentStatusBadge extends StatelessWidget {
  const _AssessmentStatusBadge({required this.label, required this.style});

  final String label;
  final _AssessmentStatusStyle style;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: style.backgroundColor,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: style.color,
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _AssessmentMetadata extends StatelessWidget {
  const _AssessmentMetadata({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 15, color: AppColors.mutedText),
        const SizedBox(width: 5),
        Text(
          text,
          style: const TextStyle(color: AppColors.mutedText, fontSize: 13),
        ),
      ],
    );
  }
}

String _subjectResult(GradeSubject subject) {
  if (subject.assessmentMode == AssessmentMode.numeric) {
    if (subject.termAverage case final average?) {
      return 'Điểm trung bình học kỳ: ${formatGradeScore(average)}';
    }
    return 'Chờ cập nhật điểm học kỳ';
  }
  return subject.termResult?.label ?? 'Chờ đánh giá';
}

String _assessmentResult(GradeAssessment assessment) {
  if (assessment.score case final score?) {
    return 'Điểm: ${formatGradeScore(score)}';
  }
  if (assessment.outcome case final outcome?) {
    return 'Kết quả: ${outcome.label}';
  }
  return 'Chưa có kết quả';
}

_AssessmentStatusStyle _assessmentStatusStyle(AssessmentStatus status) {
  return switch (status) {
    AssessmentStatus.recorded => const _AssessmentStatusStyle(
      color: Color(0xFF00695C),
      backgroundColor: Color(0xFFE0F2F1),
    ),
    AssessmentStatus.makeUpRequired => const _AssessmentStatusStyle(
      color: Color(0xFFE65100),
      backgroundColor: Color(0xFFFFF3E0),
    ),
    AssessmentStatus.excused => const _AssessmentStatusStyle(
      color: Color(0xFF5E35B1),
      backgroundColor: Color(0xFFEDE7F6),
    ),
    AssessmentStatus.absentFinalized => const _AssessmentStatusStyle(
      color: Color(0xFFC62828),
      backgroundColor: Color(0xFFFFEBEE),
    ),
  };
}

final class _AssessmentStatusStyle {
  const _AssessmentStatusStyle({
    required this.color,
    required this.backgroundColor,
  });

  final Color color;
  final Color backgroundColor;
}

GradeSubject? _findSubject(List<GradeSubject> subjects, String code) {
  for (final subject in subjects) {
    if (subject.code == code) return subject;
  }
  return null;
}

String _readableDate(DateTime date) {
  return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
}
