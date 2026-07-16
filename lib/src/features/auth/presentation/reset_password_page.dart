import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../../core/constants/app_strings.dart';
import '../application/password_reset_controller.dart';
import '../domain/auth_input_policy.dart';

class ResetPasswordPage extends ConsumerStatefulWidget {
  const ResetPasswordPage({super.key});

  static const phoneFieldKey = ValueKey('reset-phone-field');
  static const otpFieldKey = ValueKey('reset-otp-field');
  static const newPasswordFieldKey = ValueKey('reset-new-password-field');
  static const confirmPasswordFieldKey = ValueKey(
    'reset-confirm-password-field',
  );

  @override
  ConsumerState<ResetPasswordPage> createState() => _ResetPasswordPageState();
}

class _ResetPasswordPageState extends ConsumerState<ResetPasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _otpController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmationController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirmation = true;

  @override
  void dispose() {
    _phoneController.dispose();
    _otpController.dispose();
    _passwordController.dispose();
    _confirmationController.dispose();
    super.dispose();
  }

  void _returnToLogin() {
    FocusManager.instance.primaryFocus?.unfocus();
    if (context.canPop()) {
      context.pop();
    } else {
      context.goNamed(AppRouteNames.login);
    }
  }

  void _handleBack(PasswordResetState state) {
    if (state.step == PasswordResetStep.phone ||
        state.step == PasswordResetStep.success) {
      _returnToLogin();
      return;
    }
    _formKey.currentState?.reset();
    _otpController.clear();
    _passwordController.clear();
    _confirmationController.clear();
    ref.read(passwordResetControllerProvider.notifier).returnToPhone();
  }

  String? _validatePhone(String? value) {
    final phone = value?.trim() ?? '';
    if (phone.isEmpty) return 'Vui lòng nhập số điện thoại.';
    if (!AuthInputPolicy.isVietnameseMobile(phone)) {
      return 'Số điện thoại di động Việt Nam không hợp lệ.';
    }
    return null;
  }

  String? _validateOtp(String? value) {
    if (value == null || value.isEmpty) return 'Vui lòng nhập mã OTP.';
    if (!RegExp(r'^\d{6}$').hasMatch(value)) {
      return 'Mã OTP phải gồm 6 chữ số.';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập mật khẩu mới.';
    }
    if (!AuthInputPolicy.isStrongPassword(value)) {
      return 'Mật khẩu cần có chữ hoa, chữ thường, số và ký tự đặc biệt.';
    }
    return null;
  }

  String? _validateConfirmation(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng xác nhận mật khẩu mới.';
    }
    if (value != _passwordController.text) {
      return 'Mật khẩu xác nhận không khớp.';
    }
    return null;
  }

  Future<void> _requestOtp() async {
    FocusManager.instance.primaryFocus?.unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) return;
    await ref
        .read(passwordResetControllerProvider.notifier)
        .requestOtp(_phoneController.text.trim());
  }

  Future<void> _verifyOtp() async {
    FocusManager.instance.primaryFocus?.unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) return;
    await ref
        .read(passwordResetControllerProvider.notifier)
        .verifyOtp(_otpController.text);
  }

  Future<void> _complete() async {
    FocusManager.instance.primaryFocus?.unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final completed = await ref
        .read(passwordResetControllerProvider.notifier)
        .complete(_passwordController.text);
    if (completed) {
      _otpController.clear();
      _passwordController.clear();
      _confirmationController.clear();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(passwordResetControllerProvider);

    return Semantics(
      label: AppStrings.resetPassword,
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            tooltip: AppStrings.back,
            onPressed: state.isLoading ? null : () => _handleBack(state),
            icon: const Icon(Icons.arrow_back),
          ),
          title: Semantics(
            header: true,
            child: const Text(
              AppStrings.resetPassword,
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(
              horizontal: AppDimensions.pageHorizontalPadding,
              vertical: 20,
            ),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  maxWidth: AppDimensions.contentMaxWidth,
                ),
                child: Form(
                  key: _formKey,
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 200),
                    child: _buildStep(state),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStep(PasswordResetState state) {
    return switch (state.step) {
      PasswordResetStep.phone => _buildPhoneStep(state),
      PasswordResetStep.otp => _buildOtpStep(state),
      PasswordResetStep.newPassword => _buildPasswordStep(state),
      PasswordResetStep.success => _buildSuccessStep(),
    };
  }

  Widget _buildPhoneStep(PasswordResetState state) {
    return _ResetStep(
      key: const ValueKey('reset-phone-step'),
      icon: Icons.phone_android,
      heading: 'Nhập số điện thoại',
      description: 'Nhà trường sẽ gửi mã OTP để xác minh tài khoản của bạn.',
      errorMessage: state.errorMessage,
      children: [
        TextFormField(
          key: ResetPasswordPage.phoneFieldKey,
          controller: _phoneController,
          enabled: !state.isLoading,
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.done,
          autofillHints: const [AutofillHints.telephoneNumber],
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(11),
          ],
          decoration: const InputDecoration(
            labelText: AppStrings.phoneNumber,
            prefixIcon: Icon(Icons.phone, color: AppColors.primary),
          ),
          validator: _validatePhone,
          onFieldSubmitted: (_) {
            if (!state.isLoading) _requestOtp();
          },
        ),
        const SizedBox(height: 24),
        _SubmitButton(
          label: 'Gửi mã OTP',
          isLoading: state.isLoading,
          onPressed: _requestOtp,
        ),
        const SizedBox(height: 12),
        Center(
          child: TextButton(
            onPressed: state.isLoading ? null : _returnToLogin,
            child: const Text(AppStrings.backToLogin),
          ),
        ),
      ],
    );
  }

  Widget _buildOtpStep(PasswordResetState state) {
    final expiryMinutes = ((state.expiresIn ?? 0) / 60).ceil();
    return _ResetStep(
      key: const ValueKey('reset-otp-step'),
      icon: Icons.mark_email_read_outlined,
      heading: 'Xác minh mã OTP',
      description:
          'Nhập mã 6 chữ số đã gửi đến ${state.phoneNumber}. Mã có hiệu lực khoảng $expiryMinutes phút.',
      errorMessage: state.errorMessage,
      children: [
        _OtpInput(
          inputKey: ResetPasswordPage.otpFieldKey,
          controller: _otpController,
          enabled: !state.isLoading,
          validator: _validateOtp,
          onSubmitted: state.isLoading ? null : _verifyOtp,
        ),
        const SizedBox(height: 24),
        _SubmitButton(
          label: 'Xác minh OTP',
          isLoading: state.isLoading,
          onPressed: _verifyOtp,
        ),
        const SizedBox(height: 12),
        Center(
          child: TextButton(
            onPressed: state.isLoading ? null : () => _handleBack(state),
            child: const Text('Đổi số điện thoại'),
          ),
        ),
      ],
    );
  }

  Widget _buildPasswordStep(PasswordResetState state) {
    return _ResetStep(
      key: const ValueKey('reset-password-step'),
      icon: Icons.lock_reset,
      heading: 'Tạo mật khẩu mới',
      description:
          'Dùng ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.',
      errorMessage: state.errorMessage,
      children: [
        TextFormField(
          key: ResetPasswordPage.newPasswordFieldKey,
          controller: _passwordController,
          enabled: !state.isLoading,
          obscureText: _obscurePassword,
          textInputAction: TextInputAction.next,
          autofillHints: const [AutofillHints.newPassword],
          decoration: InputDecoration(
            labelText: AppStrings.newPassword,
            prefixIcon: const Icon(Icons.lock, color: AppColors.primary),
            suffixIcon: IconButton(
              tooltip: _obscurePassword
                  ? 'Hiện mật khẩu mới'
                  : 'Ẩn mật khẩu mới',
              onPressed: state.isLoading
                  ? null
                  : () => setState(() {
                      _obscurePassword = !_obscurePassword;
                    }),
              icon: Icon(
                _obscurePassword ? Icons.visibility_off : Icons.visibility,
              ),
            ),
          ),
          validator: _validatePassword,
        ),
        const SizedBox(height: 16),
        TextFormField(
          key: ResetPasswordPage.confirmPasswordFieldKey,
          controller: _confirmationController,
          enabled: !state.isLoading,
          obscureText: _obscureConfirmation,
          textInputAction: TextInputAction.done,
          autofillHints: const [AutofillHints.newPassword],
          decoration: InputDecoration(
            labelText: AppStrings.confirmNewPassword,
            prefixIcon: const Icon(
              Icons.lock_outline,
              color: AppColors.primary,
            ),
            suffixIcon: IconButton(
              tooltip: _obscureConfirmation
                  ? 'Hiện mật khẩu xác nhận'
                  : 'Ẩn mật khẩu xác nhận',
              onPressed: state.isLoading
                  ? null
                  : () => setState(() {
                      _obscureConfirmation = !_obscureConfirmation;
                    }),
              icon: Icon(
                _obscureConfirmation ? Icons.visibility_off : Icons.visibility,
              ),
            ),
          ),
          validator: _validateConfirmation,
          onFieldSubmitted: (_) {
            if (!state.isLoading) _complete();
          },
        ),
        const SizedBox(height: 24),
        _SubmitButton(
          label: AppStrings.resetPassword,
          isLoading: state.isLoading,
          onPressed: _complete,
        ),
      ],
    );
  }

  Widget _buildSuccessStep() {
    return _ResetStep(
      key: const ValueKey('reset-success-step'),
      icon: Icons.check_circle_outline,
      heading: 'Đặt lại mật khẩu thành công',
      description: 'Bạn có thể đăng nhập bằng mật khẩu mới.',
      children: [
        const SizedBox(height: 8),
        SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: _returnToLogin,
            child: const Text(AppStrings.backToLogin),
          ),
        ),
      ],
    );
  }
}

class _OtpInput extends StatefulWidget {
  const _OtpInput({
    required this.inputKey,
    required this.controller,
    required this.enabled,
    required this.validator,
    required this.onSubmitted,
  });

  final Key inputKey;
  final TextEditingController controller;
  final bool enabled;
  final FormFieldValidator<String> validator;
  final VoidCallback? onSubmitted;

  @override
  State<_OtpInput> createState() => _OtpInputState();
}

class _OtpInputState extends State<_OtpInput> {
  final _formFieldKey = GlobalKey<FormFieldState<String>>();
  late final List<TextEditingController> _digitControllers;
  late final List<FocusNode> _focusNodes;

  @override
  void initState() {
    super.initState();
    _digitControllers = List.generate(6, (_) => TextEditingController());
    _focusNodes = List.generate(6, (_) => FocusNode());
  }

  @override
  void dispose() {
    for (final controller in _digitControllers) {
      controller.dispose();
    }
    for (final node in _focusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  void _onChanged(int index, String value) {
    _syncValue();
    if (value.isNotEmpty && index < 5) {
      _focusNodes[index + 1].requestFocus();
    } else if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }
  }

  void _syncValue() {
    final value = _digitControllers.map((item) => item.text).join();
    widget.controller.value = TextEditingValue(
      text: value,
      selection: TextSelection.collapsed(offset: value.length),
    );
    _formFieldKey.currentState?.didChange(value);
  }

  @override
  Widget build(BuildContext context) {
    return FormField<String>(
      key: _formFieldKey,
      initialValue: widget.controller.text,
      validator: (_) => widget.validator(widget.controller.text),
      builder: (field) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          LayoutBuilder(
            builder: (context, constraints) {
              final width = ((constraints.maxWidth - 50) / 6).clamp(42.0, 50.0);
              return Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (var index = 0; index < 6; index++)
                    Semantics(
                      label: 'Mã OTP ${index + 1}',
                      key: ValueKey('otp-box-$index'),
                      child: SizedBox(
                        width: width,
                        height: 58,
                        child: TextField(
                          key: index == 0
                              ? widget.inputKey
                              : ValueKey('otp-digit-$index'),
                          controller: _digitControllers[index],
                          focusNode: _focusNodes[index],
                          enabled: widget.enabled,
                          keyboardType: TextInputType.number,
                          textInputAction: index == 5
                              ? TextInputAction.done
                              : TextInputAction.next,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w700,
                          ),
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(1),
                          ],
                          decoration: InputDecoration(
                            counterText: '',
                            contentPadding: EdgeInsets.zero,
                            filled: true,
                            fillColor: AppColors.surface,
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(13),
                            ),
                          ),
                          onChanged: (value) => _onChanged(index, value),
                          onSubmitted: (_) {
                            if (index == 5) widget.onSubmitted?.call();
                          },
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
          if (field.hasError) ...[
            const SizedBox(height: 8),
            Text(
              field.errorText!,
              style: TextStyle(
                color: Theme.of(context).colorScheme.error,
                fontSize: 12,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _ResetStep extends StatelessWidget {
  const _ResetStep({
    super.key,
    required this.icon,
    required this.heading,
    required this.description,
    required this.children,
    this.errorMessage,
  });

  final IconData icon;
  final String heading;
  final String description;
  final String? errorMessage;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Center(
          child: Icon(
            icon,
            semanticLabel: heading,
            size: 84,
            color: AppColors.primary,
          ),
        ),
        const SizedBox(height: 20),
        Semantics(
          header: true,
          child: Text(
            heading,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          description,
          style: const TextStyle(color: AppColors.mutedText, fontSize: 14),
        ),
        if (errorMessage case final message?) ...[
          const SizedBox(height: 16),
          Semantics(
            label: 'Thông báo đặt lại mật khẩu',
            liveRegion: true,
            container: true,
            child: Text(
              message,
              key: const ValueKey('reset-error-message'),
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ),
        ],
        const SizedBox(height: 24),
        ...children,
      ],
    );
  }
}

class _SubmitButton extends StatelessWidget {
  const _SubmitButton({
    required this.label,
    required this.isLoading,
    required this.onPressed,
  });

  final String label;
  final bool isLoading;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: isLoading ? null : onPressed,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (isLoading) ...[
              const SizedBox.square(
                dimension: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              ),
              const SizedBox(width: 10),
            ],
            Text(label),
          ],
        ),
      ),
    );
  }
}
