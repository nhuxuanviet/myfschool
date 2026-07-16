import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/events_providers.dart';
import '../domain/school_event.dart';

class EventDetailsPage extends ConsumerWidget {
  const EventDetailsPage({required this.eventId, super.key});

  final String eventId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final event = ref.watch(eventDetailsProvider(eventId));
    return event.when(
      loading: _EventDetailsLoading.new,
      error: (_, _) => _EventDetailsError(
        onRetry: () => ref.invalidate(eventDetailsProvider(eventId)),
      ),
      data: (value) => _EventDetailsScaffold(event: value),
    );
  }
}

class _EventDetailsLoading extends StatelessWidget {
  const _EventDetailsLoading();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      body: Center(
        child: Semantics(
          label: 'Đang tải chi tiết sự kiện',
          liveRegion: true,
          child: const CircularProgressIndicator(color: AppColors.primary),
        ),
      ),
    );
  }
}

class _EventDetailsError extends StatelessWidget {
  const _EventDetailsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.pageBackground,
      appBar: AppBar(
        title: const Text('Chi tiết sự kiện'),
        leading: const _BackToEventsButton(),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(AppDimensions.pageHorizontalPadding),
          child: Semantics(
            label: 'Không thể tải chi tiết sự kiện',
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
                  'Không thể tải chi tiết sự kiện. Vui lòng thử lại.',
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

class _EventDetailsScaffold extends ConsumerWidget {
  const _EventDetailsScaffold({required this.event});

  final SchoolEvent event;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mutation = ref.watch(eventRegistrationControllerProvider);
    final isSubmitting = mutation.isSubmitting;
    final errorMessage = mutation.activeEventId == event.id
        ? mutation.errorMessage
        : null;
    return Semantics(
      label: 'Chi tiết sự kiện ${event.title}',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: AppBar(
          title: const ExcludeSemantics(child: Text('Chi tiết sự kiện')),
          leading: const _BackToEventsButton(),
        ),
        body: RefreshIndicator(
          color: AppColors.primary,
          onRefresh: () => ref.refresh(eventDetailsProvider(event.id).future),
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
                        ClipRRect(
                          borderRadius: BorderRadius.circular(18),
                          child: Image.asset(
                            AppAssets.eventStemHero,
                            height: 260,
                            fit: BoxFit.cover,
                          ),
                        ),
                        const SizedBox(height: 18),
                        _EventHeader(event: event),
                        const SizedBox(height: 16),
                        _EventInformation(event: event),
                        const SizedBox(height: 16),
                        _RegistrationInformation(event: event),
                        if (errorMessage case final message?) ...[
                          const SizedBox(height: 16),
                          _RegistrationError(message: message),
                        ],
                        if (event.canRegister || event.canCancel) ...[
                          const SizedBox(height: 20),
                          if (isSubmitting) const _RegistrationLoading(),
                          if (isSubmitting) const SizedBox(height: 12),
                          _RegistrationActions(
                            event: event,
                            isSubmitting: isSubmitting,
                          ),
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

class _BackToEventsButton extends StatelessWidget {
  const _BackToEventsButton();

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: 'Quay lại sự kiện',
      onPressed: () => context.go(AppRoutes.events),
      icon: const Icon(Icons.arrow_back),
    );
  }
}

class _EventHeader extends StatelessWidget {
  const _EventHeader({required this.event});

  final SchoolEvent event;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label:
          '${event.title}. ${event.category.label}. '
          '${event.registrationStatus.label}.',
      container: true,
      child: Container(
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.border),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _DetailCategoryBadge(category: event.category),
            const SizedBox(height: 12),
            Text(
              event.title,
              style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            Text(
              event.description,
              style: const TextStyle(color: AppColors.mutedText, height: 1.45),
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailCategoryBadge extends StatelessWidget {
  const _DetailCategoryBadge({required this.category});

  final EventCategory category;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: AppColors.primarySoft,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        category.label,
        style: const TextStyle(
          color: AppColors.primary,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _EventInformation extends StatelessWidget {
  const _EventInformation({required this.event});

  final SchoolEvent event;

  @override
  Widget build(BuildContext context) {
    return _DetailSection(
      title: 'Thông tin sự kiện',
      child: Column(
        children: [
          _DetailRow(
            icon: Icons.schedule_outlined,
            label: 'Thời gian',
            value: formatEventDateRange(event.startsAt, event.endsAt),
          ),
          const SizedBox(height: 14),
          const _DetailRow(
            icon: Icons.public_outlined,
            label: 'Múi giờ',
            value: eventsSchoolTimeZoneLabel,
          ),
          const SizedBox(height: 14),
          _DetailRow(
            icon: Icons.location_on_outlined,
            label: 'Địa điểm',
            value: event.location,
          ),
          if (event.audienceGradeLevel case final gradeLevel?) ...[
            const SizedBox(height: 14),
            _DetailRow(
              icon: Icons.school_outlined,
              label: 'Đối tượng',
              value: 'Học sinh khối $gradeLevel',
            ),
          ],
        ],
      ),
    );
  }
}

class _RegistrationInformation extends StatelessWidget {
  const _RegistrationInformation({required this.event});

  final SchoolEvent event;

  @override
  Widget build(BuildContext context) {
    return _DetailSection(
      title: 'Đăng ký',
      child: Column(
        children: [
          _DetailRow(
            icon: Icons.how_to_reg_outlined,
            label: 'Trạng thái',
            value: event.registrationStatus.label,
          ),
          const SizedBox(height: 14),
          _DetailRow(
            icon: Icons.groups_outlined,
            label: 'Số lượng',
            value: _registrationCountText(event),
          ),
          if (event.registrationDeadline case final deadline?) ...[
            const SizedBox(height: 14),
            _DetailRow(
              icon: Icons.event_available_outlined,
              label: 'Hạn đăng ký',
              value: formatEventDateTime(deadline),
            ),
          ],
          if (event.cancellationDeadline case final deadline?) ...[
            const SizedBox(height: 14),
            _DetailRow(
              icon: Icons.event_busy_outlined,
              label: 'Hạn hủy đăng ký',
              value: formatEventDateTime(deadline),
            ),
          ],
        ],
      ),
    );
  }
}

class _DetailSection extends StatelessWidget {
  const _DetailSection({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
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
              title,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 19, color: AppColors.primary),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: const TextStyle(
                  color: AppColors.mutedText,
                  fontSize: 12,
                ),
              ),
              const SizedBox(height: 2),
              Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
            ],
          ),
        ),
      ],
    );
  }
}

class _RegistrationError extends StatelessWidget {
  const _RegistrationError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Lỗi thao tác sự kiện: $message',
      container: true,
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: const Color(0xFFFFEBEE),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Text(message, style: const TextStyle(color: Color(0xFFC62828))),
      ),
    );
  }
}

class _RegistrationLoading extends StatelessWidget {
  const _RegistrationLoading();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Đang xử lý đăng ký sự kiện',
      container: true,
      liveRegion: true,
      child: Center(
        child: SizedBox.square(
          dimension: 28,
          child: CircularProgressIndicator(
            strokeWidth: 3,
            color: AppColors.primary,
          ),
        ),
      ),
    );
  }
}

class _RegistrationActions extends ConsumerWidget {
  const _RegistrationActions({required this.event, required this.isSubmitting});

  final SchoolEvent event;
  final bool isSubmitting;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (event.canRegister) {
      return _RegistrationActionButton(
        label: 'Đăng ký sự kiện',
        icon: Icons.how_to_reg,
        onPressed: isSubmitting
            ? null
            : () => ref
                  .read(eventRegistrationControllerProvider.notifier)
                  .register(event.id),
      );
    }
    if (event.canCancel) {
      return _RegistrationActionButton(
        label: 'Hủy đăng ký',
        icon: Icons.cancel_outlined,
        isDestructive: true,
        onPressed: isSubmitting
            ? null
            : () => ref
                  .read(eventRegistrationControllerProvider.notifier)
                  .cancel(event.id),
      );
    }
    return const SizedBox.shrink();
  }
}

class _RegistrationActionButton extends StatelessWidget {
  const _RegistrationActionButton({
    required this.label,
    required this.icon,
    required this.onPressed,
    this.isDestructive = false,
  });

  final String label;
  final IconData icon;
  final VoidCallback? onPressed;
  final bool isDestructive;

  @override
  Widget build(BuildContext context) {
    final foregroundColor = isDestructive
        ? const Color(0xFFC62828)
        : Colors.white;
    final backgroundColor = isDestructive
        ? const Color(0xFFFFEBEE)
        : AppColors.primary;
    return Semantics(
      label: label,
      container: true,
      button: true,
      enabled: onPressed != null,
      onTap: onPressed,
      excludeSemantics: true,
      child: SizedBox(
        width: double.infinity,
        child: ElevatedButton.icon(
          onPressed: onPressed,
          icon: Icon(icon),
          label: Text(label),
          style: ElevatedButton.styleFrom(
            backgroundColor: backgroundColor,
            foregroundColor: foregroundColor,
          ),
        ),
      ),
    );
  }
}

String _registrationCountText(SchoolEvent event) {
  final capacity = event.capacity;
  return capacity == null
      ? '${event.registeredCount} học sinh đã đăng ký'
      : '${event.registeredCount}/$capacity học sinh đã đăng ký';
}
