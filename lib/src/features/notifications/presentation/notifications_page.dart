import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../home/application/home_providers.dart';
import '../../home/domain/home_dashboard.dart';

class NotificationsPage extends ConsumerWidget {
  const NotificationsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboard = ref.watch(homeDashboardProvider);
    return Semantics(
      label: 'Thông báo học sinh',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            tooltip: 'Quay lại trang chủ',
            onPressed: () => context.go(AppRoutes.home),
            icon: const Icon(Icons.arrow_back),
          ),
          title: const Text('Thông báo'),
        ),
        body: dashboard.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => Center(
            child: FilledButton.icon(
              onPressed: () => ref.invalidate(homeDashboardProvider),
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ),
          data: (value) =>
              _NotificationsList(announcements: value.announcements),
        ),
      ),
    );
  }
}

class _NotificationsList extends StatelessWidget {
  const _NotificationsList({required this.announcements});

  final List<HomeAnnouncement> announcements;

  @override
  Widget build(BuildContext context) {
    final items = [
      ...announcements
          .where(_isVisibleStudentAnnouncement)
          .map(_NotificationItem.fromAnnouncement),
      const _NotificationItem(
        title: 'Điểm môn Toán đã được cập nhật',
        body: 'Giáo viên vừa công bố kết quả đánh giá mới.',
        kind: _NotificationKind.grade,
        timeLabel: 'Hôm nay',
      ),
      const _NotificationItem(
        title: 'Đơn xin nghỉ học đã được tiếp nhận',
        body: 'Phòng hành chính đang xử lý đơn của em.',
        kind: _NotificationKind.form,
        timeLabel: 'Hôm qua',
      ),
      const _NotificationItem(
        title: 'Thay đổi phòng học',
        body: 'Tiết Ngữ văn chuyển sang phòng P201.',
        kind: _NotificationKind.schedule,
        timeLabel: '2 ngày trước',
      ),
    ];
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(
          maxWidth: AppDimensions.contentMaxWidth,
        ),
        child: ListView.separated(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          itemCount: items.length,
          separatorBuilder: (_, _) => const SizedBox(height: 12),
          itemBuilder: (context, index) =>
              _NotificationCard(item: items[index]),
        ),
      ),
    );
  }
}

class _NotificationCard extends StatelessWidget {
  const _NotificationCard({required this.item});

  final _NotificationItem item;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '${item.title}. ${item.body}',
      button: true,
      excludeSemantics: true,
      child: Material(
        color: AppColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: AppColors.border),
        ),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => context.go(item.kind.route),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: item.kind.color.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(13),
                  ),
                  child: Icon(item.kind.icon, color: item.kind.color),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.title,
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(item.body),
                      const SizedBox(height: 8),
                      Text(
                        item.timeLabel,
                        style: const TextStyle(
                          color: AppColors.mutedText,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: AppColors.mutedText),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

enum _NotificationKind {
  schedule(Icons.calendar_month_outlined, AppColors.info, AppRoutes.schedule),
  grade(Icons.bar_chart_rounded, AppColors.primary, AppRoutes.grades),
  form(Icons.description_outlined, AppColors.success, AppRoutes.forms),
  general(Icons.notifications_none_rounded, AppColors.warning, AppRoutes.home);

  const _NotificationKind(this.icon, this.color, this.route);

  final IconData icon;
  final Color color;
  final String route;
}

final class _NotificationItem {
  const _NotificationItem({
    required this.title,
    required this.body,
    required this.kind,
    required this.timeLabel,
  });

  factory _NotificationItem.fromAnnouncement(HomeAnnouncement announcement) {
    final normalized = '${announcement.title} ${announcement.body}'
        .toLowerCase();
    final kind = normalized.contains('điểm')
        ? _NotificationKind.grade
        : normalized.contains('đơn')
        ? _NotificationKind.form
        : normalized.contains('lịch') || normalized.contains('phòng')
        ? _NotificationKind.schedule
        : _NotificationKind.general;
    return _NotificationItem(
      title: announcement.title,
      body: announcement.body,
      kind: kind,
      timeLabel: _relativeTime(announcement.publishedAt),
    );
  }

  final String title;
  final String body;
  final _NotificationKind kind;
  final String timeLabel;
}

String _relativeTime(DateTime value) {
  final days = DateTime.now().difference(value).inDays;
  if (days <= 0) return 'Hôm nay';
  if (days == 1) return 'Hôm qua';
  return '$days ngày trước';
}

bool _isVisibleStudentAnnouncement(HomeAnnouncement announcement) {
  final normalized = announcement.title.toLowerCase();
  return !normalized.contains('chưa đến thời điểm') &&
      !normalized.contains('đã hết hạn');
}
