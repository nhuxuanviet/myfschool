import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../app/student_app_bar.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../auth/application/auth_controller.dart';
import '../../assistant/application/assistant_providers.dart';
import '../../home/application/home_providers.dart';
import '../../home/domain/home_dashboard.dart';

class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final dashboard = ref.watch(homeDashboardProvider);
    final state = switch (auth) {
      AsyncData(:final value) => value,
      _ => const AuthState(),
    };
    return Semantics(
      label: 'Trang cá nhân',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        appBar: studentAppBar(context: context, title: 'Cá nhân'),
        body: dashboard.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => Center(
            child: FilledButton.icon(
              onPressed: () => ref.invalidate(homeDashboardProvider),
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ),
          data: (value) => _ProfileContent(
            student: value.student,
            isSigningOut: state.isSigningOut,
            onLogout: () async {
              ref.invalidate(assistantControllerProvider);
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) context.go(AppRoutes.login);
            },
          ),
        ),
      ),
    );
  }
}

class _ProfileContent extends StatelessWidget {
  const _ProfileContent({
    required this.student,
    required this.isSigningOut,
    required this.onLogout,
  });

  final HomeStudent student;
  final bool isSigningOut;
  final Future<void> Function() onLogout;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(
          maxWidth: AppDimensions.contentMaxWidth,
        ),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
          children: [
            Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(22),
              ),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 42,
                    backgroundColor: Colors.white,
                    child: ClipOval(
                      child: Image.asset(
                        AppAssets.studentAvatar,
                        width: 78,
                        height: 78,
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  Text(
                    student.fullName,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 22,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    '${student.className} · Mã HS ${student.studentCode}',
                    style: const TextStyle(color: Colors.white),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            OutlinedButton.icon(
              onPressed: isSigningOut ? null : onLogout,
              icon: isSigningOut
                  ? const SizedBox.square(
                      dimension: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.logout),
              label: const Text('Đăng xuất'),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.primary,
                minimumSize: const Size.fromHeight(52),
                side: const BorderSide(color: AppColors.primary),
              ),
            ),
            const SizedBox(height: 18),
            _Section(
              title: 'Thông tin học sinh',
              children: [
                _InfoRow(
                  icon: Icons.badge_outlined,
                  label: 'Mã học sinh',
                  value: student.studentCode,
                ),
                _InfoRow(
                  icon: Icons.school_outlined,
                  label: 'Khối',
                  value: 'Khối ${student.gradeLevel}',
                ),
                _InfoRow(
                  icon: Icons.groups_outlined,
                  label: 'Lớp',
                  value: student.className,
                ),
                const _InfoRow(
                  icon: Icons.location_city_outlined,
                  label: 'Trường',
                  value: 'THPT FSchool Hà Nội',
                ),
              ],
            ),
            const SizedBox(height: 18),
            _Section(
              title: 'Tiện ích tài khoản',
              children: [
                _MenuRow(
                  icon: Icons.notifications_none_rounded,
                  label: 'Thông báo',
                  onTap: () => context.go(AppRoutes.notifications),
                ),
                _MenuRow(
                  icon: Icons.calendar_month_outlined,
                  label: 'Lịch học của tôi',
                  onTap: () => context.go(AppRoutes.schedule),
                ),
                _MenuRow(
                  icon: Icons.bar_chart_rounded,
                  label: 'Kết quả học tập',
                  onTap: () => context.go(AppRoutes.grades),
                ),
                _MenuRow(
                  icon: Icons.description_outlined,
                  label: 'Đơn từ của tôi',
                  onTap: () => context.go(AppRoutes.forms),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Text(
              title,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
          ),
          ...children,
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppColors.primary),
      title: Text(label),
      trailing: Text(
        value,
        style: const TextStyle(fontWeight: FontWeight.w600),
      ),
    );
  }
}

class _MenuRow extends StatelessWidget {
  const _MenuRow({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppColors.primary),
      title: Text(label),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }
}
