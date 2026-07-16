import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/student_forms_providers.dart';
import '../domain/student_form.dart';

class StudentFormCreatePage extends ConsumerStatefulWidget {
  const StudentFormCreatePage({super.key});

  @override
  ConsumerState<StudentFormCreatePage> createState() =>
      _StudentFormCreatePageState();
}

class _StudentFormCreatePageState extends ConsumerState<StudentFormCreatePage> {
  final _reasonController = TextEditingController();
  final _startsOnController = TextEditingController();
  final _endsOnController = TextEditingController();
  StudentFormType? _type;
  String? _validationMessage;

  @override
  void dispose() {
    _reasonController.dispose();
    _startsOnController.dispose();
    _endsOnController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mutation = ref.watch(studentFormMutationProvider);
    return Semantics(
      label: 'Tạo đơn học sinh',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.pageBackground,
        appBar: AppBar(
          title: const ExcludeSemantics(child: Text('Tạo đơn mới')),
          leading: IconButton(
            tooltip: 'Quay lại đơn từ',
            onPressed: () => context.go(AppRoutes.forms),
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
                  const _CreateSteps(),
                  const SizedBox(height: 32),
                  const Text(
                    'Thông tin đơn',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 16),
                  _FormTypeButton(
                    selected: _type,
                    onSelected: (type) {
                      setState(() {
                        _type = type;
                        _validationMessage = null;
                        if (type != StudentFormType.leaveOfAbsence) {
                          _startsOnController.clear();
                          _endsOnController.clear();
                        }
                      });
                    },
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _reasonController,
                    minLines: 4,
                    maxLines: 7,
                    maxLength: 1000,
                    decoration: const InputDecoration(
                      labelText: 'Lý do',
                      hintText:
                          'Trình bày ngắn gọn lý do và thông tin cần thiết',
                      alignLabelWithHint: true,
                    ),
                  ),
                  if (_type == StudentFormType.leaveOfAbsence) ...[
                    const SizedBox(height: 8),
                    TextField(
                      controller: _startsOnController,
                      keyboardType: TextInputType.datetime,
                      decoration: const InputDecoration(
                        labelText: 'Từ ngày (YYYY-MM-DD)',
                        hintText: '2026-07-15',
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _endsOnController,
                      keyboardType: TextInputType.datetime,
                      decoration: const InputDecoration(
                        labelText: 'Đến ngày (YYYY-MM-DD)',
                        hintText: '2026-07-16',
                      ),
                    ),
                  ],
                  if (_validationMessage case final message?) ...[
                    const SizedBox(height: 14),
                    _CreateFormError(message: message),
                  ],
                  if (mutation.errorMessage case final message?) ...[
                    const SizedBox(height: 14),
                    _CreateFormError(message: message),
                  ],
                  const SizedBox(height: 24),
                  Semantics(
                    label: 'Gửi đơn',
                    button: true,
                    enabled: !mutation.isSubmitting,
                    excludeSemantics: true,
                    child: FilledButton.icon(
                      onPressed: mutation.isSubmitting ? null : _submit,
                      icon: mutation.isSubmitting
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.send_outlined),
                      label: Text(
                        mutation.isSubmitting ? 'Đang gửi...' : 'Gửi đơn',
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _submit() async {
    final type = _type;
    final reason = _reasonController.text.trim();
    DateTime? startsOn;
    DateTime? endsOn;
    if (type == null) {
      setState(() => _validationMessage = 'Vui lòng chọn loại đơn.');
      return;
    }
    if (reason.isEmpty) {
      setState(() => _validationMessage = 'Vui lòng nhập lý do.');
      return;
    }
    if (type == StudentFormType.leaveOfAbsence) {
      startsOn = tryParseStudentFormDate(_startsOnController.text);
      endsOn = tryParseStudentFormDate(_endsOnController.text);
      if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
        setState(() {
          _validationMessage =
              'Khoảng ngày nghỉ không hợp lệ. Hãy dùng định dạng YYYY-MM-DD.';
        });
        return;
      }
    }
    setState(() => _validationMessage = null);
    final details = await ref
        .read(studentFormMutationProvider.notifier)
        .create(type: type, reason: reason, startsOn: startsOn, endsOn: endsOn);
    if (!mounted || details == null) return;
    context.goNamed(
      AppRouteNames.formDetails,
      pathParameters: {'formId': details.summary.id},
    );
  }
}

class _CreateSteps extends StatelessWidget {
  const _CreateSteps();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const _StepCircle(label: '1', active: true),
        Expanded(child: Container(height: 1, color: AppColors.primary)),
        const _StepCircle(label: '2'),
        Expanded(child: Container(height: 1, color: AppColors.border)),
        const _StepCircle(label: '3'),
      ],
    );
  }
}

class _StepCircle extends StatelessWidget {
  const _StepCircle({required this.label, this.active = false});

  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 40,
      height: 40,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: active ? AppColors.primary : Colors.white,
        shape: BoxShape.circle,
        border: Border.all(
          color: active ? AppColors.primary : AppColors.border,
        ),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: active ? Colors.white : AppColors.mutedText,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _FormTypeButton extends StatelessWidget {
  const _FormTypeButton({required this.selected, required this.onSelected});

  final StudentFormType? selected;
  final ValueChanged<StudentFormType> onSelected;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Loại đơn',
      button: true,
      excludeSemantics: true,
      child: OutlinedButton.icon(
        icon: const Icon(Icons.description_outlined),
        label: Align(
          alignment: Alignment.centerLeft,
          child: Text(selected?.label ?? 'Chọn loại đơn'),
        ),
        onPressed: () async {
          final type = await showModalBottomSheet<StudentFormType>(
            context: context,
            isScrollControlled: true,
            showDragHandle: true,
            builder: (sheetContext) => SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Chọn loại đơn',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),
                    for (final item in StudentFormType.values)
                      Semantics(
                        label: 'Chọn loại đơn ${item.label}',
                        button: true,
                        selected: selected == item,
                        excludeSemantics: true,
                        child: TextButton(
                          onPressed: () => Navigator.pop(sheetContext, item),
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: Text(item.label),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          );
          if (type != null && context.mounted) onSelected(type);
        },
      ),
    );
  }
}

class _CreateFormError extends StatelessWidget {
  const _CreateFormError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Lỗi tạo đơn: $message',
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
