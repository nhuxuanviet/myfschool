import 'package:flutter/material.dart';

import '../constants/app_colors.dart';

class ActivityMediaCard extends StatelessWidget {
  const ActivityMediaCard({
    required this.imagePath,
    required this.content,
    required this.onTap,
    super.key,
  });

  final String imagePath;
  final Widget content;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppColors.border),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: Image.asset(
                imagePath,
                width: 88,
                height: 96,
                fit: BoxFit.cover,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(child: content),
          ],
        ),
      ),
    );
  }
}
