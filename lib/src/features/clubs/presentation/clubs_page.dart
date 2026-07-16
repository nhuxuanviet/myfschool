import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../app/student_app_bar.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../../core/widgets/activity_media_card.dart';
import '../application/clubs_providers.dart';
import '../domain/school_club.dart';

class ClubsPage extends ConsumerWidget {
  const ClubsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final feed = ref.watch(clubsFeedProvider);
    return Semantics(
      label: 'Câu lạc bộ',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: studentAppBar(
          context: context,
          title: 'Hoạt động',
          actions: [
            IconButton(
              tooltip: 'Thông báo',
              onPressed: () => context.go(AppRoutes.notifications),
              icon: const Icon(Icons.notifications_none_rounded),
            ),
            const SizedBox(width: 12),
          ],
        ),
        body: feed.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => Center(
            child: OutlinedButton.icon(
              onPressed: () => ref.invalidate(clubsFeedProvider),
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ),
          data: (value) => _ClubsContent(feed: value),
        ),
      ),
    );
  }
}

class _ClubsContent extends ConsumerWidget {
  const _ClubsContent({required this.feed});
  final ClubsFeed feed;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return RefreshIndicator(
      onRefresh: () => ref.refresh(clubsFeedProvider.future),
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(
          AppDimensions.pageHorizontalPadding,
          20,
          AppDimensions.pageHorizontalPadding,
          36,
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
                            bottom: BorderSide(color: AppColors.border),
                          ),
                        ),
                        child: TextButton(
                          onPressed: () => context.go(AppRoutes.events),
                          child: const Text(
                            'Sự kiện',
                            style: TextStyle(
                              color: AppColors.text,
                              fontSize: 17,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                    ),
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
                          onPressed: () => context.go(AppRoutes.clubs),
                          child: const Text(
                            'Câu lạc bộ',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                if (feed.clubs.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                      'Chưa có câu lạc bộ phù hợp.',
                      textAlign: TextAlign.center,
                    ),
                  )
                else
                  for (var index = 0; index < feed.clubs.length; index++) ...[
                    _ClubCard(club: feed.clubs[index], index: index),
                    const SizedBox(height: 12),
                  ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ClubCard extends StatelessWidget {
  const _ClubCard({required this.club, required this.index});
  final SchoolClub club;
  final int index;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'CLB ${club.name}. Trạng thái ${club.membershipStatus.label}.',
      container: true,
      explicitChildNodes: true,
      child: Semantics(
        label: 'Xem CLB ${club.id}',
        button: true,
        excludeSemantics: true,
        child: ActivityMediaCard(
          imagePath: _image,
          onTap: () => context.goNamed(
            AppRouteNames.clubDetails,
            pathParameters: {'clubId': club.id},
          ),
          content: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                club.name,
                style: const TextStyle(
                  fontSize: 16,
                  height: 1.2,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                club.category.label,
                style: const TextStyle(
                  color: AppColors.mutedText,
                  fontSize: 14,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(
                    Icons.groups_outlined,
                    color: AppColors.mutedText,
                    size: 18,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    '${club.activeMemberCount} thành viên',
                    style: const TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String get _image => switch (index % 4) {
    0 => AppAssets.clubRoboticsThumb,
    1 => AppAssets.clubMusic,
    2 => AppAssets.clubDebate,
    _ => AppAssets.clubPhotography,
  };
}

class ClubDetailsPage extends ConsumerWidget {
  const ClubDetailsPage({required this.clubId, super.key});
  final String clubId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final value = ref.watch(clubDetailsProvider(clubId));
    return value.when(
      loading: () =>
          const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (_, _) => Scaffold(
        appBar: _appBar(context),
        body: Center(
          child: OutlinedButton(
            onPressed: () => ref.invalidate(clubDetailsProvider(clubId)),
            child: const Text('Thử lại'),
          ),
        ),
      ),
      data: (club) => _ClubDetailsScaffold(club: club),
    );
  }

  AppBar _appBar(BuildContext context) => AppBar(
    title: const Text('Chi tiết CLB'),
    leading: IconButton(
      tooltip: 'Quay lại câu lạc bộ',
      onPressed: () => context.go(AppRoutes.clubs),
      icon: const Icon(Icons.arrow_back),
    ),
  );
}

class _ClubDetailsScaffold extends ConsumerWidget {
  const _ClubDetailsScaffold({required this.club});
  final SchoolClub club;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mutation = ref.watch(clubMutationProvider);
    return Semantics(
      label: 'Chi tiết CLB ${club.id}',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: AppBar(
          title: const ExcludeSemantics(child: Text('Chi tiết CLB')),
          leading: IconButton(
            tooltip: 'Quay lại câu lạc bộ',
            onPressed: () => context.go(AppRoutes.clubs),
            icon: const Icon(Icons.arrow_back),
          ),
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(AppDimensions.pageHorizontalPadding),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                maxWidth: AppDimensions.contentMaxWidth,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(18),
                    child: Image.asset(
                      AppAssets.clubRoboticsHero,
                      height: 230,
                      fit: BoxFit.cover,
                    ),
                  ),
                  const SizedBox(height: 22),
                  Text(
                    club.name,
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Trạng thái: ${club.membershipStatus.label}',
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      const Icon(
                        Icons.science_outlined,
                        color: AppColors.primary,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          club.category.label,
                          style: const TextStyle(
                            color: AppColors.mutedText,
                            fontSize: 17,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      const Icon(
                        Icons.groups_outlined,
                        color: AppColors.mutedText,
                      ),
                      const SizedBox(width: 12),
                      Text(
                        '${club.activeMemberCount} thành viên',
                        style: const TextStyle(
                          color: AppColors.mutedText,
                          fontSize: 17,
                        ),
                      ),
                    ],
                  ),
                  const Divider(height: 38),
                  const Text(
                    'Giới thiệu',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    club.description,
                    style: const TextStyle(fontSize: 16, height: 1.5),
                  ),
                  const Divider(height: 38),
                  const Text(
                    'Sinh hoạt',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    club.meetingSchedule,
                    style: const TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 16,
                    ),
                  ),
                  Text(
                    club.location,
                    style: const TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 16,
                    ),
                  ),
                  if (club.applicationDeadline case final deadline?)
                    Text('Hạn đăng ký: ${formatClubInstant(deadline)}'),
                  if (mutation.errorMessage case final message?) ...[
                    const SizedBox(height: 14),
                    Text(
                      message,
                      style: const TextStyle(color: Color(0xFFC62828)),
                    ),
                  ],
                  const SizedBox(height: 24),
                  if (club.canApply)
                    _ClubActionButton(
                      label: 'Đăng ký CLB',
                      disabled: mutation.isSubmitting,
                      onPressed: () => ref
                          .read(clubMutationProvider.notifier)
                          .apply(club.id),
                    ),
                  if (club.canWithdraw)
                    _ClubActionButton(
                      label: 'Rút đơn CLB',
                      disabled: mutation.isSubmitting,
                      onPressed: () => ref
                          .read(clubMutationProvider.notifier)
                          .withdraw(club.id),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ClubActionButton extends StatelessWidget {
  const _ClubActionButton({
    required this.label,
    required this.disabled,
    required this.onPressed,
  });
  final String label;
  final bool disabled;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => Semantics(
    label: label,
    button: true,
    enabled: !disabled,
    excludeSemantics: true,
    child: FilledButton(
      onPressed: disabled ? null : onPressed,
      child: Text(disabled ? 'Đang xử lý...' : label),
    ),
  );
}
