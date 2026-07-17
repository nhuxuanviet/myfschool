import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../features/auth/application/auth_controller.dart';
import '../data/teacher_api.dart';

/// Shared shell for the teacher screens: a title, and whatever the tab shows.
class _TeacherScaffold extends StatelessWidget {
  const _TeacherScaffold({
    required this.title,
    required this.subtitle,
    required this.child,
  });

  final String title;
  final String subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) {
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
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.mutedText,
                    ),
                  ),
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

/// Renders a future, with the three states every screen here needs.
class _AsyncSection<T> extends StatelessWidget {
  const _AsyncSection({
    required this.value,
    required this.builder,
    required this.emptyMessage,
    required this.onRetry,
  });

  final AsyncValue<T> value;
  final Widget Function(T data) builder;
  final String emptyMessage;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return value.when(
      loading: () => const Center(
        child: CircularProgressIndicator(color: AppColors.primary),
      ),
      error: (error, _) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, color: AppColors.mutedText),
              const SizedBox(height: 12),
              const Text(
                'Không tải được dữ liệu.',
                style: TextStyle(color: AppColors.mutedText),
              ),
              const SizedBox(height: 12),
              FilledButton(onPressed: onRetry, child: const Text('Thử lại')),
            ],
          ),
        ),
      ),
      data: (data) {
        if (data is List && data.isEmpty) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Text(
                emptyMessage,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.mutedText),
              ),
            ),
          );
        }
        return builder(data);
      },
    );
  }
}

class TeacherOverviewPage extends ConsumerWidget {
  const TeacherOverviewPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final classes = ref.watch(teacherClassesProvider);
    final forms = ref.watch(homeroomFormsProvider);

    return _TeacherScaffold(
      title: 'Tổng quan',
      subtitle: 'Lớp được phân công và việc cần xử lý',
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
        children: [
          _StatRow(
            children: [
              _StatCard(
                label: 'Lớp - môn',
                value: classes.maybeWhen(
                  data: (items) => '${items.length}',
                  orElse: () => '—',
                ),
                icon: Icons.class_outlined,
              ),
              _StatCard(
                label: 'Đơn chờ duyệt',
                value: forms.maybeWhen(
                  data: (items) =>
                      '${items.where((form) => form.isOpen).length}',
                  orElse: () => '—',
                ),
                icon: Icons.assignment_outlined,
              ),
            ],
          ),
          const SizedBox(height: 20),
          const Text(
            'Lớp bạn phụ trách',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          classes.maybeWhen(
            data: (items) => Column(
              children: items
                  .take(4)
                  .map(
                    (item) => _ClassTile(
                      item: item,
                      onTap: () => _openStudents(context, item),
                    ),
                  )
                  .toList(),
            ),
            orElse: () => const SizedBox(
              height: 80,
              child: Center(
                child: CircularProgressIndicator(color: AppColors.primary),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _openStudents(BuildContext context, AssignedClass item) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => ClassStudentsPage(assignedClass: item),
      ),
    );
  }
}

class _StatRow extends StatelessWidget {
  const _StatRow({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (var index = 0; index < children.length; index++) ...[
          Expanded(child: children[index]),
          if (index != children.length - 1) const SizedBox(width: 12),
        ],
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: AppColors.primary, size: 20),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w700),
          ),
          Text(
            label,
            style: const TextStyle(fontSize: 12, color: AppColors.mutedText),
          ),
        ],
      ),
    );
  }
}

class _ClassTile extends StatelessWidget {
  const _ClassTile({required this.item, this.onTap});

  final AssignedClass item;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
      ),
      child: ListTile(
        onTap: onTap,
        title: Text(
          '${item.classCode} · ${item.subjectName}',
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
        subtitle: Text('${item.studentCount} học sinh'),
        trailing: item.homeroom
            ? const Chip(
                label: Text('Chủ nhiệm', style: TextStyle(fontSize: 11)),
                visualDensity: VisualDensity.compact,
              )
            : const Icon(Icons.chevron_right_rounded),
      ),
    );
  }
}

class TeacherSchedulePage extends ConsumerWidget {
  const TeacherSchedulePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final schedule = ref.watch(teacherScheduleProvider(null));

    return _TeacherScaffold(
      title: 'Lịch dạy',
      subtitle: 'Tiết dạy trong tuần này',
      child: _AsyncSection<TeachingWeek>(
        value: schedule,
        emptyMessage: 'Tuần này chưa có tiết dạy nào.',
        onRetry: () => ref.invalidate(teacherScheduleProvider(null)),
        builder: (week) {
          if (week.lessons.isEmpty) {
            return const Center(
              child: Text(
                'Tuần này chưa có tiết dạy nào.',
                style: TextStyle(color: AppColors.mutedText),
              ),
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
            itemCount: week.lessons.length,
            itemBuilder: (context, index) {
              final lesson = week.lessons[index];
              return Container(
                margin: const EdgeInsets.only(bottom: 8),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.border),
                ),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: AppColors.primary.withValues(alpha: 0.1),
                    child: Text(
                      '${lesson.periodNumber}',
                      style: const TextStyle(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  title: Text(
                    lesson.subjectName,
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      decoration: lesson.isCancelled
                          ? TextDecoration.lineThrough
                          : null,
                    ),
                  ),
                  subtitle: Text(
                    '${lesson.classCode} · ${lesson.startTime} - ${lesson.endTime}'
                    '${lesson.room == null ? '' : ' · ${lesson.room}'}',
                  ),
                  trailing: lesson.isCancelled
                      ? const Text(
                          'Đã huỷ',
                          style: TextStyle(fontSize: 12, color: Colors.red),
                        )
                      : null,
                ),
              );
            },
          );
        },
      ),
    );
  }
}

class TeacherClassesPage extends ConsumerWidget {
  const TeacherClassesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final classes = ref.watch(teacherClassesProvider);
    final forms = ref.watch(homeroomFormsProvider);
    final openForms = forms.maybeWhen(
      data: (items) => items.where((form) => form.isOpen).toList(),
      orElse: () => const <HomeroomLeaveForm>[],
    );

    return _TeacherScaffold(
      title: 'Lớp',
      subtitle: 'Lớp được phân công và lớp chủ nhiệm',
      child: _AsyncSection<List<AssignedClass>>(
        value: classes,
        emptyMessage: 'Bạn chưa được phân công lớp nào.',
        onRetry: () => ref.invalidate(teacherClassesProvider),
        builder: (items) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
          children: [
            // The homeroom section only exists for a homeroom teacher; a teacher
            // without one should not see an empty box asking about it.
            if (openForms.isNotEmpty) ...[
              const Text(
                'Đơn xin nghỉ chờ duyệt',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              ...openForms.map((form) => _LeaveFormTile(form: form)),
              const SizedBox(height: 20),
            ],
            const Text(
              'Lớp - môn được phân công',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            ...items.map(
              (item) => _ClassTile(
                item: item,
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => ClassStudentsPage(assignedClass: item),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LeaveFormTile extends ConsumerStatefulWidget {
  const _LeaveFormTile({required this.form});

  final HomeroomLeaveForm form;

  @override
  ConsumerState<_LeaveFormTile> createState() => _LeaveFormTileState();
}

class _LeaveFormTileState extends ConsumerState<_LeaveFormTile> {
  bool _busy = false;

  Future<void> _decide(bool approve) async {
    setState(() => _busy = true);
    try {
      await ref
          .read(teacherApiProvider)
          .decideForm(widget.form.id, approve: approve);
      ref.invalidate(homeroomFormsProvider);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final form = widget.form;
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${form.studentFullName} · ${form.studentCode}',
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 4),
          Text(
            form.reason,
            style: const TextStyle(fontSize: 13, color: AppColors.mutedText),
          ),
          const SizedBox(height: 4),
          Text(
            // Who asked changes how the request reads, so it is on the card.
            form.submittedByRole == 'PARENT'
                ? 'Phụ huynh nộp'
                : 'Học sinh nộp',
            style: const TextStyle(fontSize: 11, color: AppColors.mutedText),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _busy ? null : () => _decide(false),
                  child: const Text('Từ chối'),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: FilledButton(
                  onPressed: _busy ? null : () => _decide(true),
                  child: const Text('Duyệt'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class ClassStudentsPage extends ConsumerWidget {
  const ClassStudentsPage({required this.assignedClass, super.key});

  final AssignedClass assignedClass;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final students = ref.watch(classStudentsProvider(assignedClass.classId));

    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      appBar: AppBar(
        title: Text('${assignedClass.classCode} · ${assignedClass.subjectName}'),
      ),
      body: _AsyncSection<List<ClassStudent>>(
        value: students,
        emptyMessage: 'Lớp này chưa có học sinh.',
        onRetry: () =>
            ref.invalidate(classStudentsProvider(assignedClass.classId)),
        builder: (items) => ListView.builder(
          padding: const EdgeInsets.all(20),
          itemCount: items.length,
          itemBuilder: (context, index) {
            final student = items[index];
            return Container(
              margin: const EdgeInsets.only(bottom: 8),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.border),
              ),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: AppColors.primary.withValues(alpha: 0.1),
                  child: Text(
                    '${index + 1}',
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontSize: 12,
                    ),
                  ),
                ),
                title: Text(student.fullName),
                subtitle: Text(student.studentCode),
              ),
            );
          },
        ),
      ),
    );
  }
}

/// Read-only on mobile: entering marks is done on the web, so this shows the
/// books the teacher is responsible for and nothing that edits them.
class TeacherGradeBooksPage extends ConsumerWidget {
  const TeacherGradeBooksPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final classes = ref.watch(teacherClassesProvider);

    return _TeacherScaffold(
      title: 'Sổ điểm',
      subtitle: 'Xem sổ điểm lớp - môn bạn phụ trách',
      child: _AsyncSection<List<AssignedClass>>(
        value: classes,
        emptyMessage: 'Bạn chưa được phân công lớp nào.',
        onRetry: () => ref.invalidate(teacherClassesProvider),
        builder: (items) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(
                color: AppColors.primary.withValues(alpha: 0.06),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Row(
                children: [
                  Icon(
                    Icons.info_outline_rounded,
                    size: 18,
                    color: AppColors.primary,
                  ),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Nhập điểm thực hiện trên bản web. Trên điện thoại chỉ xem.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            ...items.map(
              (item) => _ClassTile(
                item: item,
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => ClassStudentsPage(assignedClass: item),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class TeacherProfilePage extends ConsumerWidget {
  const TeacherProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homerooms = ref.watch(teacherHomeroomsProvider);

    return _TeacherScaffold(
      title: 'Cá nhân',
      subtitle: 'Thông tin và phiên đăng nhập',
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
        children: [
          homerooms.maybeWhen(
            data: (items) => items.isEmpty
                ? const SizedBox.shrink()
                : Container(
                    padding: const EdgeInsets.all(16),
                    margin: const EdgeInsets.only(bottom: 12),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Lớp chủ nhiệm',
                          style: TextStyle(fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 8),
                        ...items.map(
                          (item) => Text(
                            '${item.classCode} · ${item.studentCount} học sinh',
                            style: const TextStyle(
                              color: AppColors.mutedText,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
            orElse: () => const SizedBox.shrink(),
          ),
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
