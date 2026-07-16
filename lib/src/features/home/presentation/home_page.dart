import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../auth/application/auth_controller.dart';
import '../application/home_providers.dart';
import '../domain/home_dashboard.dart';

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final authState = switch (auth) {
      AsyncData(:final value) => value,
      _ => const AuthState(),
    };
    final dashboard = ref.watch(homeDashboardProvider);
    final fallbackName = authState.session?.student.fullName ?? 'Học sinh';

    return Semantics(
      label: 'Trang chủ',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        body: dashboard.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) =>
              _LoadError(onRetry: () => ref.invalidate(homeDashboardProvider)),
          data: (value) => _Dashboard(
            dashboard: value,
            fallbackName: fallbackName,
            onRefresh: () => ref.refresh(homeDashboardProvider.future),
          ),
        ),
      ),
    );
  }
}

class _Dashboard extends StatelessWidget {
  const _Dashboard({
    required this.dashboard,
    required this.fallbackName,
    required this.onRefresh,
  });

  final HomeDashboard dashboard;
  final String fallbackName;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final student = dashboard.student;
    return RefreshIndicator(
      color: AppColors.primary,
      onRefresh: onRefresh,
      child: ListView(
        padding: EdgeInsets.zero,
        children: [
          _Header(student: student, fallbackName: fallbackName),
          Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                maxWidth: AppDimensions.contentMaxWidth,
              ),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
                child: Column(
                  children: [
                    const _NextLesson(),
                    const SizedBox(height: 14),
                    const _Tasks(),
                    const SizedBox(height: 14),
                    const _Upcoming(),
                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.student, required this.fallbackName});

  final HomeStudent student;
  final String fallbackName;

  @override
  Widget build(BuildContext context) {
    final name = student.fullName.isEmpty ? fallbackName : student.fullName;
    return Semantics(
      label:
          'Hồ sơ học sinh: $name, ${student.className}, khối ${student.gradeLevel}, mã học sinh ${student.studentCode}',
      container: true,
      explicitChildNodes: true,
      child: ClipPath(
        key: const Key('home-header'),
        clipper: const _HeaderArcClipper(),
        child: Container(
          height: 176,
          padding: const EdgeInsets.fromLTRB(24, 32, 20, 34),
          color: AppColors.primary,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Xin chào,',
                      style: TextStyle(color: Colors.white, fontSize: 16),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 25,
                        fontWeight: FontWeight.w700,
                        letterSpacing: -0.4,
                      ),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      '${student.className} · Trường THPT FSchool Hà Nội',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                    ),
                  ],
                ),
              ),
              Semantics(
                label: 'Thông báo',
                button: true,
                excludeSemantics: true,
                child: IconButton(
                  onPressed: () => context.go(AppRoutes.notifications),
                  icon: const Badge(
                    smallSize: 7,
                    child: Icon(
                      Icons.notifications_none_rounded,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
              Semantics(
                label: 'Mở trang cá nhân',
                button: true,
                excludeSemantics: true,
                child: InkWell(
                  onTap: () => context.go(AppRoutes.more),
                  customBorder: const CircleBorder(),
                  child: CircleAvatar(
                    radius: 29,
                    backgroundColor: Colors.white,
                    child: ClipOval(
                      child: Image.asset(
                        AppAssets.studentAvatar,
                        width: 54,
                        height: 54,
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HeaderArcClipper extends CustomClipper<Path> {
  const _HeaderArcClipper();

  static const double _arcDepth = 20;

  @override
  Path getClip(Size size) {
    final edgeY = size.height - _arcDepth;
    return Path()
      ..lineTo(size.width, 0)
      ..lineTo(size.width, edgeY)
      ..quadraticBezierTo(size.width / 2, size.height + _arcDepth, 0, edgeY)
      ..close();
  }

  @override
  bool shouldReclip(covariant _HeaderArcClipper oldClipper) => false;
}

class _NextLesson extends StatelessWidget {
  const _NextLesson();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Mở lịch học cho tiết học tiếp theo',
      button: true,
      excludeSemantics: true,
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () => context.go(AppRoutes.schedule),
          child: Padding(
            padding: const EdgeInsets.all(15),
            child: Column(
              children: [
                const _SectionHeader(
                  icon: Icons.menu_book_outlined,
                  title: 'Tiết học tiếp theo',
                  trailing: _Pill(
                    text: 'Còn 15 phút',
                    color: AppColors.primary,
                  ),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Container(
                      width: 72,
                      height: 72,
                      decoration: BoxDecoration(
                        color: AppColors.primarySoft,
                        borderRadius: BorderRadius.circular(15),
                      ),
                      child: const Icon(
                        Icons.calculate_outlined,
                        color: AppColors.primary,
                        size: 38,
                      ),
                    ),
                    const SizedBox(width: 15),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Toán học',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          SizedBox(height: 5),
                          Text(
                            'P203 · Thầy Phạm Minh',
                            style: TextStyle(color: AppColors.mutedText),
                          ),
                          SizedBox(height: 5),
                          Row(
                            children: [
                              Icon(
                                Icons.schedule,
                                color: AppColors.primary,
                                size: 18,
                              ),
                              SizedBox(width: 6),
                              Text(
                                '09:55 – 10:40',
                                style: TextStyle(color: AppColors.mutedText),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const Icon(Icons.chevron_right, color: AppColors.mutedText),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Tasks extends StatelessWidget {
  const _Tasks();

  @override
  Widget build(BuildContext context) {
    final tasks = [
      (
        'Điểm môn Toán đã được cập nhật',
        'Hôm nay · 09:15',
        'Mới',
        AppColors.info,
      ),
      (
        'Đăng ký CLB Robotics',
        'Hạn: 15/05/2026',
        'Còn 2 ngày',
        AppColors.success,
      ),
      (
        'Đóng học phí học kỳ II',
        'Hạn: 16/05/2026',
        'Quá hạn 1 ngày',
        AppColors.warning,
      ),
    ];
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 14, 14, 5),
        child: Column(
          children: [
            _SectionHeader(
              icon: Icons.assignment_outlined,
              title: 'Việc cần làm',
              onTap: () => context.go(AppRoutes.notifications),
              trailing: const Text(
                'Xem tất cả',
                style: TextStyle(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            const SizedBox(height: 8),
            for (var index = 0; index < tasks.length; index++) ...[
              if (index > 0) const Divider(height: 1),
              ListTile(
                contentPadding: EdgeInsets.zero,
                minLeadingWidth: 38,
                leading: Icon(
                  Icons.description_outlined,
                  color: tasks[index].$4,
                ),
                title: Text(
                  tasks[index].$1,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                subtitle: Text(
                  tasks[index].$2,
                  style: const TextStyle(fontSize: 12),
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _Pill(text: tasks[index].$3, color: tasks[index].$4),
                    const Icon(Icons.chevron_right, size: 21),
                  ],
                ),
                onTap: () => context.go(
                  index == 1 ? AppRoutes.clubs : AppRoutes.notifications,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _Upcoming extends StatelessWidget {
  const _Upcoming();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 14, 14, 5),
        child: Column(
          children: [
            _SectionHeader(
              title: 'Sắp tới',
              onTap: () => context.go(AppRoutes.events),
              trailing: const Text(
                'Xem thêm',
                style: TextStyle(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            const SizedBox(height: 8),
            _UpcomingRow(
              day: '15',
              image: AppAssets.eventStemHero,
              title: 'Ngày hội STEM 2026',
              detail: 'Thứ Năm, 15/05/2026 · 08:00 – 16:30',
              location: 'Sân trường FSchool Hà Nội',
              onTap: () => context.go(AppRoutes.events),
            ),
            const Divider(height: 1),
            _UpcomingRow(
              day: '20',
              image: AppAssets.eventStudyAbroad,
              title: 'Cuộc thi Hùng biện tiếng Anh',
              detail: 'Thứ Ba, 20/05/2026 · 08:00',
              location: 'Phòng Đa năng',
              onTap: () => context.go(AppRoutes.events),
            ),
          ],
        ),
      ),
    );
  }
}

class _UpcomingRow extends StatelessWidget {
  const _UpcomingRow({
    required this.day,
    required this.image,
    required this.title,
    required this.detail,
    required this.location,
    required this.onTap,
  });

  final String day;
  final String image;
  final String title;
  final String detail;
  final String location;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            SizedBox(
              width: 42,
              child: Column(
                children: [
                  Text(
                    day,
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontSize: 24,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const Text(
                    'TH5',
                    style: TextStyle(
                      color: AppColors.primary,
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: Image.asset(
                image,
                width: 62,
                height: 55,
                fit: BoxFit.cover,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  Text(
                    detail,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 11.5,
                      color: AppColors.mutedText,
                    ),
                  ),
                  Row(
                    children: [
                      const Icon(
                        Icons.location_on_outlined,
                        size: 14,
                        color: AppColors.mutedText,
                      ),
                      const SizedBox(width: 3),
                      Expanded(
                        child: Text(
                          location,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 11.5,
                            color: AppColors.mutedText,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: AppColors.mutedText),
          ],
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({
    this.icon,
    required this.title,
    required this.trailing,
    this.onTap,
  });

  final IconData? icon;
  final String title;
  final Widget trailing;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final row = Row(
      children: [
        if (icon != null) ...[
          Icon(icon, color: AppColors.mutedText, size: 21),
          const SizedBox(width: 8),
        ],
        Expanded(
          child: Text(
            title,
            style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
          ),
        ),
        trailing,
      ],
    );
    if (onTap == null) return row;
    return Semantics(
      button: true,
      label: '$title, mở chi tiết',
      excludeSemantics: true,
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: row,
        ),
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: TextStyle(
          color: color,
          fontSize: 11.5,
          fontWeight: FontWeight.w600,
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
      child: FilledButton.icon(
        onPressed: onRetry,
        icon: const Icon(Icons.refresh),
        label: const Text('Thử tải lại trang chủ'),
      ),
    );
  }
}
