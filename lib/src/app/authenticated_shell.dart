import 'package:flutter/material.dart';

import '../core/constants/app_assets.dart';
import '../core/constants/app_colors.dart';
import '../features/assistant/presentation/assistant_page.dart';

final class AuthenticatedShell extends StatelessWidget {
  const AuthenticatedShell({
    required this.child,
    required this.location,
    required this.navigationLocations,
    required this.onDestinationSelected,
    this.hideNavigation = false,
    super.key,
  });

  final Widget child;
  final String location;
  final List<String> navigationLocations;
  final ValueChanged<int> onDestinationSelected;
  final bool hideNavigation;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: child,
      floatingActionButton: Semantics(
        label: 'Mở trợ lý AI',
        button: true,
        excludeSemantics: true,
        child: FloatingActionButton(
          heroTag: 'student-assistant',
          tooltip: 'Mở trợ lý AI',
          elevation: 3,
          backgroundColor: Colors.white,
          foregroundColor: AppColors.primary,
          shape: CircleBorder(
            side: BorderSide(color: AppColors.primary.withValues(alpha: 0.25)),
          ),
          onPressed: () => _openAssistant(context),
          child: ClipOval(
            child: Image.asset(
              AppAssets.aiRobot,
              width: 48,
              height: 48,
              fit: BoxFit.cover,
            ),
          ),
        ),
      ),
      bottomNavigationBar: hideNavigation
          ? null
          : NavigationBar(
              selectedIndex: _selectedIndex(location),
              onDestinationSelected: onDestinationSelected,
              destinations: const [
                NavigationDestination(
                  icon: Icon(Icons.home_outlined),
                  selectedIcon: Icon(Icons.home),
                  label: 'Trang chủ',
                ),
                NavigationDestination(
                  icon: Icon(Icons.calendar_month_outlined),
                  selectedIcon: Icon(Icons.calendar_month),
                  label: 'Lịch học',
                ),
                NavigationDestination(
                  icon: Icon(Icons.bar_chart_outlined),
                  selectedIcon: Icon(Icons.bar_chart_rounded),
                  label: 'Kết quả',
                ),
                NavigationDestination(
                  icon: Icon(Icons.workspace_premium_outlined),
                  selectedIcon: Icon(Icons.workspace_premium),
                  label: 'Hoạt động',
                ),
                NavigationDestination(
                  icon: Icon(Icons.person_outline_rounded),
                  selectedIcon: Icon(Icons.person_rounded),
                  label: 'Cá nhân',
                ),
              ],
            ),
    );
  }

  Future<void> _openAssistant(BuildContext context) {
    return showModalBottomSheet<void>(
      context: context,
      useSafeArea: true,
      isScrollControlled: true,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (context) => const FractionallySizedBox(
        heightFactor: 0.86,
        child: ClipRRect(
          borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
          child: AssistantPage(embedded: true),
        ),
      ),
    );
  }

  int _selectedIndex(String currentLocation) {
    final matchingIndex = navigationLocations.indexOf(currentLocation);
    if (matchingIndex >= 0) return matchingIndex;
    for (var index = 0; index < navigationLocations.length; index += 1) {
      if (currentLocation.startsWith('${navigationLocations[index]}/')) {
        return index;
      }
    }
    if (currentLocation.startsWith('/clubs') ||
        currentLocation.startsWith('/forms')) {
      return 3;
    }
    return 4;
  }
}
