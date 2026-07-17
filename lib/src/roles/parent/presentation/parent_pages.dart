import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../features/auth/application/auth_controller.dart';
import '../data/parent_api.dart';

class _ParentScaffold extends ConsumerWidget {
  const _ParentScaffold({
    required this.title,
    required this.child,
    this.showChildPicker = true,
  });

  final String title;
  final Widget child;
  final bool showChildPicker;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(selectedChildProvider);
    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  if (showChildPicker && selected != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      '${selected.fullName} · ${selected.className}',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.mutedText,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            Expanded(child: child),
          ],
        ),
      ),
    );
  }
}

/// Every screen needs the same three states, and one of them is "no child yet".
class _ChildGate extends ConsumerWidget {
  const _ChildGate({required this.builder});

  final Widget Function(ChildSummary child) builder;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final children = ref.watch(parentChildrenProvider);
    return children.when(
      loading: () => const Center(
        child: CircularProgressIndicator(color: AppColors.primary),
      ),
      error: (error, _) => _Message(
        text: 'Không tải được danh sách con.',
        onRetry: () => ref.invalidate(parentChildrenProvider),
      ),
      data: (items) {
        if (items.isEmpty) {
          // A guardian with no link sees nothing, and should be told why rather
          // than left staring at an empty screen.
          return const _Message(
            text: 'Tài khoản của bạn chưa được liên kết với học sinh nào.\n'
                'Vui lòng liên hệ nhà trường.',
          );
        }
        final selected = ref.watch(selectedChildProvider);
        if (selected == null) {
          return const Center(
            child: CircularProgressIndicator(color: AppColors.primary),
          );
        }
        return builder(selected);
      },
    );
  }
}

class _Message extends StatelessWidget {
  const _Message({required this.text, this.onRetry});

  final String text;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              text,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.mutedText),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              FilledButton(onPressed: onRetry, child: const Text('Thử lại')),
            ],
          ],
        ),
      ),
    );
  }
}

class _Card extends StatelessWidget {
  const _Card({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
      ),
      child: child,
    );
  }
}

class ParentOverviewPage extends ConsumerWidget {
  const ParentOverviewPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ParentScaffold(
      title: 'Tổng quan',
      child: _ChildGate(
        builder: (child) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
          children: [
            _Card(
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 24,
                    backgroundColor: AppColors.primary.withValues(alpha: 0.1),
                    child: Text(
                      child.fullName.characters.first,
                      style: const TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          child.fullName,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          '${child.studentCode} · Lớp ${child.className}',
                          style: const TextStyle(
                            fontSize: 13,
                            color: AppColors.mutedText,
                          ),
                        ),
                        Text(
                          'Quan hệ: ${child.relationshipLabel}',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.mutedText,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              'Bạn xem được gì',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            const _Card(
              child: Text(
                'Thời khoá biểu và kết quả học tập đã được giáo viên công bố. '
                'Điểm chưa công bố sẽ không hiển thị.',
                style: TextStyle(fontSize: 13, color: AppColors.mutedText),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ParentTimetablePage extends ConsumerWidget {
  const ParentTimetablePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ParentScaffold(
      title: 'Lịch học',
      child: _ChildGate(
        builder: (child) {
          final timetable = ref.watch(childTimetableProvider(child.studentId));
          return timetable.when(
            loading: () => const Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
            error: (error, _) => _Message(
              text: 'Không tải được lịch học.',
              onRetry: () =>
                  ref.invalidate(childTimetableProvider(child.studentId)),
            ),
            data: (data) {
              final days = (data['days'] as List<dynamic>? ?? const []);
              if (days.isEmpty) {
                return const _Message(text: 'Tuần này chưa có lịch học.');
              }
              return ListView(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
                children: days.map((raw) {
                  final day = raw as Map<String, dynamic>;
                  final lessons = day['lessons'] as List<dynamic>? ?? const [];
                  return _Card(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          day['date']?.toString() ?? '',
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 6),
                        if (lessons.isEmpty)
                          const Text(
                            'Không có tiết',
                            style: TextStyle(
                              fontSize: 12,
                              color: AppColors.mutedText,
                            ),
                          )
                        else
                          ...lessons.map((item) {
                            final lesson = item as Map<String, dynamic>;
                            return Padding(
                              padding: const EdgeInsets.symmetric(vertical: 2),
                              child: Text(
                                'Tiết ${lesson['periodNumber']} · '
                                '${lesson['subjectName'] ?? ''}'
                                '${lesson['teacherName'] == null ? '' : ' · ${lesson['teacherName']}'}',
                                style: const TextStyle(fontSize: 13),
                              ),
                            );
                          }),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          );
        },
      ),
    );
  }
}

class ParentGradesPage extends ConsumerWidget {
  const ParentGradesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ParentScaffold(
      title: 'Kết quả',
      child: _ChildGate(
        builder: (child) {
          final grades = ref.watch(childGradesProvider(child.studentId));
          return grades.when(
            loading: () => const Center(
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
            error: (error, _) => _Message(
              text: 'Không tải được kết quả học tập.',
              onRetry: () =>
                  ref.invalidate(childGradesProvider(child.studentId)),
            ),
            data: (data) {
              final subjects =
                  (data['subjects'] as List<dynamic>? ?? const []);
              if (subjects.isEmpty) {
                return const _Message(text: 'Chưa có kết quả để hiển thị.');
              }
              return ListView(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
                children: subjects.map((raw) {
                  final subject = raw as Map<String, dynamic>;
                  final assessments =
                      subject['assessments'] as List<dynamic>? ?? const [];
                  final average = subject['termAverage'];
                  return _Card(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                subject['name']?.toString() ?? '',
                                style: const TextStyle(
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                            Text(
                              average?.toString() ??
                                  subject['termResult']?.toString() ??
                                  '—',
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          assessments.isEmpty
                              // A published book with no marks and an unpublished
                              // one look the same from here, and should: the
                              // guardian is told there is nothing to see, not why.
                              ? 'Chưa có điểm được công bố'
                              : '${assessments.length} đầu điểm',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.mutedText,
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          );
        },
      ),
    );
  }
}

class ParentFormsPage extends ConsumerWidget {
  const ParentFormsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return _ParentScaffold(
      title: 'Đơn từ',
      child: _ChildGate(
        builder: (child) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
          children: [
            _Card(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Đơn xin nghỉ cho ${child.fullName}',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'Đơn xin nghỉ do bạn nộp sẽ được giáo viên chủ nhiệm của lớp duyệt.',
                    style: TextStyle(fontSize: 13, color: AppColors.mutedText),
                  ),
                ],
              ),
            ),
            const _Card(
              child: Text(
                'Chưa có đơn nào.',
                style: TextStyle(fontSize: 13, color: AppColors.mutedText),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class ParentProfilePage extends ConsumerWidget {
  const ParentProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final children = ref.watch(parentChildrenProvider);
    final selected = ref.watch(selectedChildProvider);

    return _ParentScaffold(
      title: 'Cá nhân',
      showChildPicker: false,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
        children: [
          const Text(
            'Chọn con',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          ...children.maybeWhen(
            data: (items) => items.map(
              (item) => _Card(
                child: InkWell(
                  onTap: () => ref
                      .read(selectedChildIdProvider.notifier)
                      .select(item.studentId),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              item.fullName,
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            Text(
                              '${item.studentCode} · Lớp ${item.className}',
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppColors.mutedText,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (selected?.studentId == item.studentId)
                        const Icon(
                          Icons.check_circle_rounded,
                          color: AppColors.primary,
                        ),
                    ],
                  ),
                ),
              ),
            ),
            orElse: () => <Widget>[],
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(Icons.logout_rounded),
            label: const Text('Đăng xuất'),
          ),
        ],
      ),
    );
  }
}
