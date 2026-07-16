import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../app/student_app_bar.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/student_forms_providers.dart';
import '../domain/student_form.dart';

class StudentFormsPage extends ConsumerWidget {
  const StudentFormsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final forms = ref.watch(studentFormsFeedProvider);
    return Semantics(
      label: 'Đơn từ',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: studentAppBar(context: context, title: 'Đơn từ'),
        body: forms.when(
          loading: () => Center(
            child: Semantics(
              label: 'Đang tải đơn từ',
              liveRegion: true,
              child: CircularProgressIndicator(color: AppColors.primary),
            ),
          ),
          error: (_, _) => _FormsError(
            onRetry: () => ref.invalidate(studentFormsFeedProvider),
          ),
          data: (feed) => _FormsContent(feed: feed),
        ),
      ),
    );
  }
}

class _FormsError extends StatelessWidget {
  const _FormsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Semantics(
        label: 'Không thể tải đơn từ',
        container: true,
        explicitChildNodes: true,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, size: 48),
            const SizedBox(height: 12),
            const Text('Không thể tải danh sách đơn.'),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }
}

class _FormsContent extends ConsumerWidget {
  const _FormsContent({required this.feed});

  final StudentFormsFeed feed;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () => ref.refresh(studentFormsFeedProvider.future),
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(
          AppDimensions.pageHorizontalPadding,
          20,
          AppDimensions.pageHorizontalPadding,
          100,
        ),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              maxWidth: AppDimensions.wideContentMaxWidth,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Container(
                        decoration: const BoxDecoration(
                          border: Border(
                            bottom: BorderSide(
                              color: AppColors.primary,
                              width: 3,
                            ),
                          ),
                        ),
                        child: TextButton(
                          onPressed: () => context.go(AppRoutes.forms),
                          child: const Text(
                            'Của tôi',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: Container(
                        decoration: const BoxDecoration(
                          border: Border(
                            bottom: BorderSide(color: AppColors.border),
                          ),
                        ),
                        child: TextButton(
                          onPressed: () => context.go(AppRoutes.formCreate),
                          child: const Text(
                            'Mẫu đơn',
                            style: TextStyle(
                              color: AppColors.text,
                              fontSize: 17,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 22),
                if (feed.forms.isEmpty)
                  const _EmptyForms()
                else
                  for (final form in feed.forms) ...[
                    _StudentFormCard(form: form),
                    const SizedBox(height: 12),
                  ],
                const SizedBox(height: 18),
                Semantics(
                  label: 'Tạo đơn mới',
                  button: true,
                  excludeSemantics: true,
                  child: FilledButton.icon(
                    onPressed: () => context.go(AppRoutes.formCreate),
                    icon: const Icon(Icons.add),
                    label: const Text('Tạo đơn mới'),
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

class _StudentFormCard extends StatelessWidget {
  const _StudentFormCard({required this.form});

  final StudentFormSummary form;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Đơn ${form.type.label}. Trạng thái ${form.status.label}.',
      container: true,
      explicitChildNodes: true,
      child: Semantics(
        label: 'Xem đơn ${form.id}',
        button: true,
        excludeSemantics: true,
        child: InkWell(
          onTap: () => context.goNamed(
            AppRouteNames.formDetails,
            pathParameters: {'formId': form.id},
          ),
          borderRadius: BorderRadius.circular(16),
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        form.type.label,
                        style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        formatStudentFormInstant(
                          form.submittedAt,
                        ).split(' • ').last,
                        style: const TextStyle(color: AppColors.mutedText),
                      ),
                    ],
                  ),
                ),
                _FormStatusBadge(status: form.status),
                const SizedBox(width: 8),
                const Icon(Icons.chevron_right, color: AppColors.mutedText),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _FormStatusBadge extends StatelessWidget {
  const _FormStatusBadge({required this.status});

  final StudentFormStatus status;

  @override
  Widget build(BuildContext context) {
    final (color, background) = switch (status) {
      StudentFormStatus.approved => (
        const Color(0xFF00695C),
        const Color(0xFFE0F2F1),
      ),
      StudentFormStatus.rejected || StudentFormStatus.cancelled => (
        const Color(0xFFC62828),
        const Color(0xFFFFEBEE),
      ),
      _ => (AppColors.primary, AppColors.primarySoft),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        status.label,
        style: TextStyle(
          color: color,
          fontSize: 12,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _EmptyForms extends StatelessWidget {
  const _EmptyForms();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Chưa có đơn phù hợp',
      child: Padding(
        padding: EdgeInsets.all(28),
        child: Text(
          'Chưa có đơn phù hợp với bộ lọc.',
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.mutedText),
        ),
      ),
    );
  }
}
