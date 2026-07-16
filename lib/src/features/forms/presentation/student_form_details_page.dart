import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/student_forms_providers.dart';
import '../domain/student_form.dart';

class StudentFormDetailsPage extends ConsumerWidget {
  const StudentFormDetailsPage({required this.formId, super.key});

  final String formId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final details = ref.watch(studentFormDetailsProvider(formId));
    return details.when(
      loading: () => Scaffold(
        body: Center(
          child: Semantics(
            label: 'Đang tải chi tiết đơn',
            liveRegion: true,
            child: CircularProgressIndicator(color: AppColors.primary),
          ),
        ),
      ),
      error: (_, _) => Scaffold(
        appBar: _detailsAppBar(context),
        body: Center(
          child: Semantics(
            label: 'Không thể tải chi tiết đơn',
            child: OutlinedButton.icon(
              onPressed: () =>
                  ref.invalidate(studentFormDetailsProvider(formId)),
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ),
        ),
      ),
      data: (value) => _DetailsScaffold(details: value),
    );
  }

  PreferredSizeWidget _detailsAppBar(BuildContext context) {
    return AppBar(
      title: const Text('Chi tiết đơn'),
      leading: IconButton(
        tooltip: 'Quay lại đơn từ',
        onPressed: () => context.go(AppRoutes.forms),
        icon: const Icon(Icons.arrow_back),
      ),
    );
  }
}

class _DetailsScaffold extends ConsumerWidget {
  const _DetailsScaffold({required this.details});

  final StudentFormDetails details;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final form = details.summary;
    final mutation = ref.watch(studentFormMutationProvider);
    return Semantics(
      label: 'Chi tiết đơn ${form.id}',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: AppBar(
          title: const ExcludeSemantics(child: Text('Chi tiết đơn')),
          leading: IconButton(
            tooltip: 'Quay lại đơn từ',
            onPressed: () => context.go(AppRoutes.forms),
            icon: const Icon(Icons.arrow_back),
          ),
        ),
        body: RefreshIndicator(
          onRefresh: () =>
              ref.refresh(studentFormDetailsProvider(form.id).future),
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(
              AppDimensions.pageHorizontalPadding,
              20,
              AppDimensions.pageHorizontalPadding,
              40,
            ),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  maxWidth: AppDimensions.contentMaxWidth,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _FormOverview(details: details),
                    const SizedBox(height: 16),
                    _FormTimeline(entries: details.timeline),
                    if (mutation.errorMessage case final message?) ...[
                      const SizedBox(height: 16),
                      Semantics(
                        label: 'Lỗi hủy đơn: $message',
                        liveRegion: true,
                        child: Text(
                          message,
                          style: const TextStyle(color: Color(0xFFC62828)),
                        ),
                      ),
                    ],
                    if (form.canCancel) ...[
                      const SizedBox(height: 20),
                      Semantics(
                        label: 'Hủy đơn',
                        button: true,
                        enabled: !mutation.isSubmitting,
                        excludeSemantics: true,
                        child: OutlinedButton.icon(
                          onPressed: mutation.isSubmitting
                              ? null
                              : () => _confirmCancel(context, ref, form.id),
                          icon: const Icon(Icons.cancel_outlined),
                          label: Text(
                            mutation.isSubmitting ? 'Đang hủy...' : 'Hủy đơn',
                          ),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: const Color(0xFFC62828),
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _confirmCancel(
    BuildContext context,
    WidgetRef ref,
    String formId,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Hủy đơn này?'),
        content: const Text('Đơn đã hủy sẽ không thể gửi lại với cùng mã.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Giữ đơn'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Xác nhận hủy đơn'),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await ref.read(studentFormMutationProvider.notifier).cancel(formId);
    }
  }
}

class _FormOverview extends StatelessWidget {
  const _FormOverview({required this.details});

  final StudentFormDetails details;

  @override
  Widget build(BuildContext context) {
    final form = details.summary;
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            form.type.label,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          Text(
            'Trạng thái: ${form.status.label}',
            style: const TextStyle(
              color: AppColors.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
          const Divider(height: 28),
          const Text('Lý do', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text(details.reason),
          if (form.startsOn case final startsOn?) ...[
            const SizedBox(height: 16),
            const Text(
              'Thời gian nghỉ',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 6),
            Text(formatStudentFormDateRange(startsOn, form.endsOn!)),
          ],
          const SizedBox(height: 16),
          Text(
            'Gửi lúc ${formatStudentFormInstant(form.submittedAt)}',
            style: const TextStyle(color: AppColors.mutedText),
          ),
        ],
      ),
    );
  }
}

class _FormTimeline extends StatelessWidget {
  const _FormTimeline({required this.entries});

  final List<StudentFormTimelineEntry> entries;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Semantics(
            header: true,
            child: Text(
              'Tiến trình xử lý',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 14),
          for (var index = 0; index < entries.length; index += 1)
            _TimelineEntry(
              entry: entries[index],
              isLast: index == entries.length - 1,
            ),
        ],
      ),
    );
  }
}

class _TimelineEntry extends StatelessWidget {
  const _TimelineEntry({required this.entry, required this.isLast});

  final StudentFormTimelineEntry entry;
  final bool isLast;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label:
          'Trạng thái ${entry.status.label}, ${formatStudentFormInstant(entry.occurredAt)}. '
          '${entry.note ?? ''}',
      container: true,
      child: Padding(
        padding: EdgeInsets.only(bottom: isLast ? 0 : 16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.circle, size: 12, color: AppColors.primary),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    entry.status.label,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  Text(
                    formatStudentFormInstant(entry.occurredAt),
                    style: const TextStyle(color: AppColors.mutedText),
                  ),
                  if (entry.note case final note?) Text(note),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
