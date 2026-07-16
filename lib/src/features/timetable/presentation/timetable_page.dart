import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';

import '../../../app/student_app_bar.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/timetable_providers.dart';
import '../domain/timetable.dart';

class TimetablePage extends ConsumerWidget {
  const TimetablePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final timetable = ref.watch(timetableProvider);
    return Semantics(
      label: 'Lịch học',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.surface,
        appBar: studentAppBar(
          context: context,
          title: 'Lịch học',
          actions: [
            IconButton(
              tooltip: 'Chọn ngày',
              onPressed: () => _selectDate(context, ref),
              icon: const Icon(Icons.calendar_today_outlined),
            ),
            IconButton(
              tooltip: 'Sao chép lịch học',
              onPressed: () => _copyTimetable(context, ref),
              icon: const Icon(Icons.ios_share_outlined),
            ),
            const SizedBox(width: 8),
          ],
        ),
        body: timetable.when(
          loading: () => Center(
            child: Semantics(
              label: 'Đang tải lịch học',
              liveRegion: true,
              child: const CircularProgressIndicator(),
            ),
          ),
          error: (_, _) =>
              _LoadError(onRetry: () => ref.invalidate(timetableProvider)),
          data: (value) => _Content(timetable: value),
        ),
      ),
    );
  }

  Future<void> _selectDate(BuildContext context, WidgetRef ref) async {
    final timetable = switch (ref.read(timetableProvider)) {
      AsyncData(:final value) => value,
      _ => null,
    };
    final initialDate =
        ref.read(timetableSelectedDateProvider) ??
        timetable?.weekStart ??
        DateTime.now();
    final selected = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(initialDate.year - 1),
      lastDate: DateTime(initialDate.year + 1),
      initialEntryMode: DatePickerEntryMode.calendarOnly,
      locale: const Locale('vi', 'VN'),
      helpText: 'Chọn ngày học',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
      builder: (context, child) {
        final theme = Theme.of(context);
        return Theme(
          data: theme.copyWith(
            colorScheme: theme.colorScheme.copyWith(
              primary: AppColors.primary,
              onPrimary: Colors.white,
              surface: Colors.white,
            ),
            datePickerTheme: DatePickerThemeData(
              backgroundColor: Colors.white,
              headerBackgroundColor: AppColors.primary,
              headerForegroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
              ),
              dividerColor: AppColors.border,
            ),
          ),
          child: child!,
        );
      },
    );
    if (selected == null) return;
    final monday = selected.subtract(Duration(days: selected.weekday - 1));
    ref.read(timetableWeekStartProvider.notifier).select(monday);
    ref.read(timetableSelectedDateProvider.notifier).select(selected);
  }

  Future<void> _copyTimetable(BuildContext context, WidgetRef ref) async {
    final timetable = switch (ref.read(timetableProvider)) {
      AsyncData(:final value) => value,
      _ => null,
    };
    if (timetable == null) return;
    final text = timetable.days
        .where((day) => day.lessons.isNotEmpty)
        .map(
          (day) =>
              '${day.dayOfWeek.label} ${_displayDate(day.date)}: '
              '${day.lessons.map((lesson) => lesson.subject.name).join(', ')}',
        )
        .join('\n');
    await Clipboard.setData(ClipboardData(text: text));
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Đã sao chép lịch học tuần này.')),
    );
  }
}

class _Content extends ConsumerWidget {
  const _Content({required this.timetable});

  final Timetable timetable;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedDate = ref.watch(timetableSelectedDateProvider);
    final selected = timetable.days.firstWhere(
      (day) => selectedDate != null && _sameDate(day.date, selectedDate),
      orElse: () => timetable.days.first,
    );
    return RefreshIndicator(
      onRefresh: () => ref.refresh(timetableProvider.future),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(
            maxWidth: AppDimensions.contentMaxWidth,
          ),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(22, 10, 22, 28),
            children: [
              _WeekHeader(timetable: timetable, selected: selected),
              const SizedBox(height: 18),
              _DaySelector(
                days: timetable.days,
                selected: selected,
                onSelected: (day) => ref
                    .read(timetableSelectedDateProvider.notifier)
                    .select(day.date),
              ),
              if (timetable.academicTerms.isEmpty) ...[
                const SizedBox(height: 18),
                const Text(
                  'Chưa có học kỳ đang hoạt động.',
                  style: TextStyle(color: AppColors.mutedText),
                ),
              ] else if (timetable.academicTerms.length > 1) ...[
                const SizedBox(height: 18),
                _TransitionTerms(terms: timetable.academicTerms),
              ],
              const SizedBox(height: 22),
              _DayTimeline(day: selected),
            ],
          ),
        ),
      ),
    );
  }
}

class _WeekHeader extends ConsumerWidget {
  const _WeekHeader({required this.timetable, required this.selected});

  final Timetable timetable;
  final TimetableDay selected;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final label =
        'Tuần ${formatTimetableDate(timetable.weekStart)} đến ${formatTimetableDate(timetable.weekEnd)}';
    return Semantics(
      label: label,
      header: true,
      explicitChildNodes: true,
      child: Row(
        children: [
          Expanded(
            child: Text(
              '${selected.dayOfWeek.label}, ${_displayDate(selected.date)}',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
            ),
          ),
          Semantics(
            label: 'Tuần trước',
            button: true,
            excludeSemantics: true,
            child: IconButton(
              visualDensity: VisualDensity.compact,
              onPressed: () => _move(ref, -7),
              icon: const Icon(Icons.chevron_left),
            ),
          ),
          Semantics(
            label: 'Tuần sau',
            button: true,
            excludeSemantics: true,
            child: IconButton(
              visualDensity: VisualDensity.compact,
              onPressed: () => _move(ref, 7),
              icon: const Icon(Icons.chevron_right),
            ),
          ),
        ],
      ),
    );
  }

  void _move(WidgetRef ref, int days) {
    ref.read(timetableSelectedDateProvider.notifier).select(null);
    ref
        .read(timetableWeekStartProvider.notifier)
        .select(timetable.weekStart.add(Duration(days: days)));
  }
}

class _DaySelector extends StatelessWidget {
  const _DaySelector({
    required this.days,
    required this.selected,
    required this.onSelected,
  });

  final List<TimetableDay> days;
  final TimetableDay selected;
  final ValueChanged<TimetableDay> onSelected;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        for (final day in days)
          Semantics(
            label: 'Chọn ngày ${formatTimetableDate(day.date)}',
            button: true,
            selected: _sameDate(day.date, selected.date),
            excludeSemantics: true,
            child: InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => onSelected(day),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 180),
                width: 42,
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  color: _sameDate(day.date, selected.date)
                      ? AppColors.primary
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(13),
                ),
                child: Column(
                  children: [
                    Text(
                      day.dayOfWeek.shortLabel,
                      style: TextStyle(
                        color: _sameDate(day.date, selected.date)
                            ? Colors.white
                            : AppColors.mutedText,
                        fontSize: 12,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${day.date.day}',
                      style: TextStyle(
                        color: _sameDate(day.date, selected.date)
                            ? Colors.white
                            : AppColors.text,
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _DayTimeline extends StatelessWidget {
  const _DayTimeline({required this.day});

  final TimetableDay day;

  static const accents = [
    Color(0xFF2582F3),
    AppColors.primary,
    Color(0xFF17A369),
    Color(0xFF13A9BA),
    Color(0xFF7B4CF4),
    Color(0xFFF39A08),
  ];

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Lịch ngày ${formatTimetableDate(day.date)}',
      container: true,
      explicitChildNodes: true,
      child: day.lessons.isEmpty
          ? Padding(
              padding: const EdgeInsets.symmetric(vertical: 60),
              child: Semantics(
                label: 'Không có tiết học trong ngày này',
                child: const Text(
                  'Không có tiết học trong ngày này.',
                  textAlign: TextAlign.center,
                ),
              ),
            )
          : Column(
              children: [
                for (var index = 0; index < day.lessons.length; index++)
                  _LessonRow(
                    lesson: day.lessons[index],
                    accent: accents[index % accents.length],
                    highlighted: index == 1,
                    isLast: index == day.lessons.length - 1,
                  ),
              ],
            ),
    );
  }
}

class _LessonRow extends StatelessWidget {
  const _LessonRow({
    required this.lesson,
    required this.accent,
    required this.highlighted,
    required this.isLast,
  });

  final TimetableLesson lesson;
  final Color accent;
  final bool highlighted;
  final bool isLast;

  @override
  Widget build(BuildContext context) {
    final details = [
      if (lesson.room != null) lesson.room!,
      if (lesson.teacherName != null) lesson.teacherName!,
    ].join(' · ');
    return Semantics(
      label:
          'Tiết ${lesson.periodNumber}: ${lesson.subject.name}. ${lesson.startTime} - ${lesson.endTime}. ${lesson.status.label}${lesson.note == null ? '' : '. ${lesson.note}'}',
      container: true,
      child: SizedBox(
        height: 94,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SizedBox(
              width: 58,
              child: Padding(
                padding: const EdgeInsets.only(top: 20),
                child: Text(
                  lesson.startTime,
                  style: TextStyle(
                    color: highlighted
                        ? AppColors.primary
                        : AppColors.mutedText,
                    fontSize: 15,
                    fontWeight: highlighted ? FontWeight.w600 : FontWeight.w400,
                  ),
                ),
              ),
            ),
            SizedBox(
              width: 20,
              child: Stack(
                alignment: Alignment.topCenter,
                children: [
                  Positioned(
                    top: 0,
                    bottom: isLast ? 68 : 0,
                    child: Container(width: 1, color: AppColors.border),
                  ),
                  Positioned(
                    top: 25,
                    child: Container(
                      width: 9,
                      height: 9,
                      decoration: BoxDecoration(
                        color: highlighted ? accent : const Color(0xFFD4D6DC),
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.symmetric(
                  horizontal: 15,
                  vertical: 13,
                ),
                decoration: BoxDecoration(
                  color: highlighted ? AppColors.primary : AppColors.surface,
                  borderRadius: BorderRadius.circular(13),
                  border: highlighted
                      ? null
                      : Border.all(color: AppColors.border),
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x0A000000),
                      blurRadius: 12,
                      offset: Offset(0, 4),
                    ),
                  ],
                ),
                child: Row(
                  children: [
                    if (!highlighted)
                      Container(
                        width: 3,
                        height: 52,
                        decoration: BoxDecoration(
                          color: accent,
                          borderRadius: BorderRadius.circular(99),
                        ),
                      ),
                    if (!highlighted) const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            lesson.subject.name,
                            style: TextStyle(
                              color: highlighted
                                  ? Colors.white
                                  : AppColors.text,
                              fontSize: 18,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            details,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: highlighted
                                  ? Colors.white
                                  : AppColors.mutedText,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TransitionTerms extends StatelessWidget {
  const _TransitionTerms({required this.terms});

  final List<TimetableAcademicTerm> terms;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Chuyển tiếp học kỳ trong tuần này',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            for (final term in terms)
              Text(
                '${term.name} • ${term.academicYear}\n${_displayDate(term.startsOn)} – ${_displayDate(term.endsOn)}',
              ),
          ],
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
      child: Semantics(
        label: 'Không thể tải lịch học',
        child: FilledButton.icon(
          onPressed: onRetry,
          icon: const Icon(Icons.refresh),
          label: const Text('Thử lại'),
        ),
      ),
    );
  }
}

String _displayDate(DateTime date) =>
    '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';

bool _sameDate(DateTime left, DateTime right) =>
    left.year == right.year &&
    left.month == right.month &&
    left.day == right.day;
