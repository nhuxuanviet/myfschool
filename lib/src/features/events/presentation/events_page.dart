import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../app/student_app_bar.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../../core/widgets/activity_media_card.dart';
import '../application/events_providers.dart';
import '../domain/school_event.dart';

class EventsPage extends ConsumerStatefulWidget {
  const EventsPage({super.key});

  @override
  ConsumerState<EventsPage> createState() => _EventsPageState();
}

class _EventsPageState extends ConsumerState<EventsPage> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();

  @override
  Widget build(BuildContext context) {
    final events = ref.watch(eventsFeedProvider);

    return Semantics(
      label: 'Sự kiện',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        key: _scaffoldKey,
        backgroundColor: AppColors.pageBackground,
        endDrawer: const Drawer(
          width: 340,
          backgroundColor: AppColors.filterBackground,
          child: _EventsFilters(),
        ),
        appBar: studentAppBar(
          context: context,
          title: 'Hoạt động',
          actions: [
            Semantics(
              label: 'Lọc sự kiện',
              button: true,
              onTap: () => _scaffoldKey.currentState?.openEndDrawer(),
              excludeSemantics: true,
              child: IconButton(
                onPressed: () => _scaffoldKey.currentState?.openEndDrawer(),
                icon: const Icon(Icons.filter_list),
              ),
            ),
            IconButton(
              tooltip: 'Thông báo',
              onPressed: () => context.go(AppRoutes.notifications),
              icon: const Icon(Icons.notifications_none_rounded),
            ),
            const SizedBox(width: 12),
          ],
        ),
        body: events.when(
          loading: _EventsLoading.new,
          error: (_, _) => _EventsLoadError(
            onRetry: () => ref.invalidate(eventsFeedProvider),
          ),
          data: (feed) => _EventsContent(feed: feed),
        ),
      ),
    );
  }
}

class _EventsLoading extends StatelessWidget {
  const _EventsLoading();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Semantics(
        label: 'Đang tải sự kiện',
        container: true,
        liveRegion: true,
        child: const CircularProgressIndicator(color: AppColors.primary),
      ),
    );
  }
}

class _EventsLoadError extends StatelessWidget {
  const _EventsLoadError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppDimensions.pageHorizontalPadding),
        child: Semantics(
          label: 'Không thể tải sự kiện',
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
                'Không thể tải sự kiện. Vui lòng kiểm tra kết nối và thử lại.',
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
    );
  }
}

class _EventsContent extends ConsumerWidget {
  const _EventsContent({required this.feed});

  final EventsFeed feed;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: () => ref.refresh(eventsFeedProvider.future),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final horizontalPadding = constraints.maxWidth >= 600 ? 32.0 : 20.0;
          return ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: EdgeInsets.fromLTRB(
              horizontalPadding,
              0,
              horizontalPadding,
              32,
            ),
            children: [
              Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(
                    maxWidth: AppDimensions.wideContentMaxWidth,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _ActivityTabs(
                        onEvents: () => context.go(AppRoutes.events),
                        onClubs: () => context.go(AppRoutes.clubs),
                      ),
                      const SizedBox(height: 24),
                      if (feed.events.isEmpty)
                        const _EmptyEvents()
                      else
                        for (final event in feed.events) ...[
                          _EventCard(
                            event: event,
                            index: feed.events.indexOf(event),
                          ),
                          const SizedBox(height: 16),
                        ],
                      const _SchoolTimeZoneNotice(),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ActivityTabs extends StatelessWidget {
  const _ActivityTabs({required this.onEvents, required this.onClubs});

  final VoidCallback onEvents;
  final VoidCallback onClubs;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Container(
            decoration: const BoxDecoration(
              border: Border(
                bottom: BorderSide(color: AppColors.primary, width: 3),
              ),
            ),
            child: TextButton(
              onPressed: onEvents,
              child: const Text(
                'Sự kiện',
                style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
              ),
            ),
          ),
        ),
        Expanded(
          child: Container(
            decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppColors.border)),
            ),
            child: TextButton(
              onPressed: onClubs,
              child: const Text(
                'Câu lạc bộ',
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
    );
  }
}

class _SchoolTimeZoneNotice extends StatelessWidget {
  const _SchoolTimeZoneNotice();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Múi giờ hiển thị: UTC+07',
      container: true,
      excludeSemantics: true,
      child: Text(
        eventsSchoolTimeZoneLabel,
        style: const TextStyle(color: AppColors.mutedText, fontSize: 13),
      ),
    );
  }
}

class _EventsFilters extends ConsumerWidget {
  const _EventsFilters();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filter = ref.watch(eventsFilterProvider);
    return SafeArea(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 12, 10, 8),
            child: Row(
              children: [
                const Expanded(
                  child: Text(
                    'Bộ lọc sự kiện',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                  ),
                ),
                IconButton(
                  tooltip: 'Đóng bộ lọc',
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.close),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Semantics(
                  label: 'Hiển thị sự kiện đã qua',
                  container: true,
                  toggled: filter.includePast,
                  excludeSemantics: true,
                  child: SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Hiển thị sự kiện đã qua'),
                    subtitle: const Text('Bao gồm sự kiện đã kết thúc'),
                    value: filter.includePast,
                    activeTrackColor: AppColors.primarySoft,
                    activeThumbColor: AppColors.primary,
                    inactiveTrackColor: const Color(0xFFFFF1EC),
                    inactiveThumbColor: const Color(0xFF8F7B76),
                    trackOutlineColor: WidgetStateProperty.resolveWith(
                      (states) => states.contains(WidgetState.selected)
                          ? AppColors.primary
                          : const Color(0xFF8F7B76),
                    ),
                    onChanged: ref
                        .read(eventsFilterProvider.notifier)
                        .setIncludePast,
                  ),
                ),
                const Divider(height: 28),
                const Text(
                  'Loại sự kiện',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 12),
                _CategoryPickerOption(
                  label: 'Tất cả',
                  selected: filter.category == null,
                  onSelected: () => ref
                      .read(eventsFilterProvider.notifier)
                      .selectCategory(null),
                ),
                const SizedBox(height: 8),
                for (final category in EventCategory.values) ...[
                  _CategoryPickerOption(
                    label: category.label,
                    selected: filter.category == category,
                    onSelected: () => ref
                        .read(eventsFilterProvider.notifier)
                        .selectCategory(category),
                  ),
                  const SizedBox(height: 8),
                ],
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: FilledButton.icon(
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.check),
              label: const Text('Xem kết quả'),
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoryPickerOption extends StatelessWidget {
  const _CategoryPickerOption({
    required this.label,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final bool selected;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Lọc: $label',
      button: true,
      selected: selected,
      onTap: onSelected,
      excludeSemantics: true,
      child: TextButton(
        onPressed: onSelected,
        style: TextButton.styleFrom(
          alignment: Alignment.centerLeft,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          backgroundColor: selected ? AppColors.primarySoft : AppColors.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(
              color: selected ? AppColors.primary : AppColors.border,
            ),
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  color: selected ? AppColors.primary : null,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            if (selected) const Icon(Icons.check, color: AppColors.primary),
          ],
        ),
      ),
    );
  }
}

class _EmptyEvents extends StatelessWidget {
  const _EmptyEvents();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Không có sự kiện phù hợp',
      container: true,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(16),
        ),
        child: const Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.event_busy_outlined, color: AppColors.mutedText),
            SizedBox(height: 8),
            Text(
              'Không có sự kiện phù hợp.',
              textAlign: TextAlign.center,
              style: TextStyle(color: AppColors.mutedText),
            ),
          ],
        ),
      ),
    );
  }
}

class _EventCard extends StatelessWidget {
  const _EventCard({required this.event, required this.index});

  final SchoolEvent event;
  final int index;

  @override
  Widget build(BuildContext context) {
    final statusStyle = _EventStatusStyle.forStatus(event.registrationStatus);
    final semanticLabel = [
      'Sự kiện ${event.title}.',
      event.category.label,
      formatEventDateRange(event.startsAt, event.endsAt),
      'Địa điểm ${event.location}.',
      event.registrationStatus.label,
      _capacityText(event),
    ].join(' ');
    return Semantics(
      label: semanticLabel,
      container: true,
      explicitChildNodes: true,
      child: Semantics(
        label: 'Xem sự kiện ${event.title}',
        button: true,
        excludeSemantics: true,
        child: ActivityMediaCard(
          imagePath: _image,
          onTap: () => _openDetails(context),
          content: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _EventStatusBadge(
                label:
                    index == 0 &&
                        event.registrationStatus ==
                            EventRegistrationStatus.registered
                    ? 'Đã đăng ký'
                    : 'Sắp diễn ra',
                style: statusStyle,
              ),
              const SizedBox(height: 7),
              Text(
                event.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 16,
                  height: 1.2,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 8),
              _EventMetadata(
                icon: Icons.calendar_today_outlined,
                text: formatEventDateTime(event.startsAt),
              ),
              const SizedBox(height: 5),
              _EventMetadata(
                icon: Icons.location_on_outlined,
                text: event.location,
              ),
            ],
          ),
        ),
      ),
    );
  }

  String get _image => switch (index % 3) {
    0 => AppAssets.eventStemHero,
    1 => AppAssets.eventStudyAbroad,
    _ => AppAssets.eventEnglishSpeech,
  };

  void _openDetails(BuildContext context) {
    context.goNamed(
      AppRouteNames.eventDetails,
      pathParameters: {'eventId': event.id},
    );
  }
}

class _EventStatusBadge extends StatelessWidget {
  const _EventStatusBadge({required this.label, required this.style});

  final String label;
  final _EventStatusStyle style;

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

class _EventMetadata extends StatelessWidget {
  const _EventMetadata({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 15, color: AppColors.mutedText),
        const SizedBox(width: 5),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(color: AppColors.mutedText, fontSize: 13),
          ),
        ),
      ],
    );
  }
}

String _capacityText(SchoolEvent event) {
  final capacity = event.capacity;
  return capacity == null
      ? 'Đã đăng ký: ${event.registeredCount}'
      : 'Đã đăng ký: ${event.registeredCount}/$capacity';
}

final class _EventStatusStyle {
  const _EventStatusStyle({
    required this.color,
    required this.backgroundColor,
    required this.borderColor,
  });

  factory _EventStatusStyle.forStatus(EventRegistrationStatus status) {
    return switch (status) {
      EventRegistrationStatus.notRegistered => const _EventStatusStyle(
        color: AppColors.primary,
        backgroundColor: AppColors.primarySoft,
        borderColor: AppColors.border,
      ),
      EventRegistrationStatus.registered => const _EventStatusStyle(
        color: Color(0xFF00695C),
        backgroundColor: Color(0xFFE0F2F1),
        borderColor: Color(0xFF80CBC4),
      ),
      EventRegistrationStatus.cancelled => const _EventStatusStyle(
        color: Color(0xFFC62828),
        backgroundColor: Color(0xFFFFEBEE),
        borderColor: Color(0xFFEF9A9A),
      ),
    };
  }

  final Color color;
  final Color backgroundColor;
  final Color borderColor;
}
