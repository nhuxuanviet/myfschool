import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_strings.dart';
import '../domain/health_check.dart';
import '../providers/health_providers.dart';

class SystemStatusIndicator extends ConsumerWidget {
  const SystemStatusIndicator({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final health = ref.watch(healthCheckProvider);

    return health.when(
      data: (value) => switch (value.status) {
        HealthStatus.up => const _Status(
          label: AppStrings.systemOnline,
          color: Colors.green,
        ),
        HealthStatus.down || HealthStatus.unknown => _Status(
          label: AppStrings.systemOffline,
          color: Theme.of(context).colorScheme.error,
          onRetry: () => ref.invalidate(healthCheckProvider),
        ),
      },
      error: (_, _) => _Status(
        label: AppStrings.systemOffline,
        color: Theme.of(context).colorScheme.error,
        onRetry: () => ref.invalidate(healthCheckProvider),
      ),
      loading: () => const _Status(
        label: AppStrings.systemChecking,
        color: AppColors.mutedText,
      ),
    );
  }
}

class _Status extends StatelessWidget {
  const _Status({required this.label, required this.color, this.onRetry});

  final String label;
  final Color color;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final content = Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: const TextStyle(color: AppColors.mutedText, fontSize: 12),
        ),
      ],
    );

    return Semantics(
      label: label,
      container: true,
      button: onRetry != null,
      child: ExcludeSemantics(
        child: onRetry == null
            ? content
            : InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: onRetry,
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: content,
                ),
              ),
      ),
    );
  }
}
