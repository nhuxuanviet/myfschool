import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../../../core/constants/app_strings.dart';
import '../../system_health/presentation/system_status_indicator.dart';
import '../application/auth_controller.dart';
import '../domain/auth_input_policy.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  static const phoneFieldKey = ValueKey('login-phone-field');
  static const passwordFieldKey = ValueKey('login-password-field');

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    FocusManager.instance.primaryFocus?.unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    final success = await ref
        .read(authControllerProvider.notifier)
        .login(
          phoneNumber: _phoneController.text.trim(),
          password: _passwordController.text,
        );
    if (success && mounted) context.goNamed(AppRouteNames.home);
  }

  String? _validatePhone(String? value) {
    final phone = value?.trim() ?? '';
    if (phone.isEmpty) {
      return 'Vui lòng nhập số điện thoại.';
    }
    if (!AuthInputPolicy.isVietnameseMobile(phone)) {
      return 'Số điện thoại di động Việt Nam không hợp lệ.';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập mật khẩu.';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    final authState = switch (auth) {
      AsyncData(:final value) => value,
      _ => const AuthState(),
    };
    final isLoading = auth.isLoading || authState.isSigningIn;

    return Semantics(
      label: AppStrings.loginScreen,
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        body: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppDimensions.pageHorizontalPadding,
                ),
                child: Center(
                  child: ConstrainedBox(
                    constraints: BoxConstraints(
                      minHeight: constraints.maxHeight,
                      maxWidth: AppDimensions.contentMaxWidth,
                    ),
                    child: IntrinsicHeight(
                      child: Form(
                        key: _formKey,
                        child: Column(
                          children: [
                            const SizedBox(height: 64),
                            Image.asset(
                              AppAssets.fptSchoolsLogo,
                              width: 180,
                              semanticLabel: AppStrings.appName,
                            ),
                            const SizedBox(height: 72),
                            TextFormField(
                              key: LoginPage.phoneFieldKey,
                              controller: _phoneController,
                              keyboardType: TextInputType.phone,
                              textInputAction: TextInputAction.next,
                              autofillHints: const [
                                AutofillHints.telephoneNumber,
                              ],
                              inputFormatters: [
                                FilteringTextInputFormatter.digitsOnly,
                                LengthLimitingTextInputFormatter(11),
                              ],
                              decoration: const InputDecoration(
                                labelText: AppStrings.phoneNumber,
                                prefixIcon: Icon(
                                  Icons.phone,
                                  color: AppColors.primary,
                                ),
                              ),
                              validator: _validatePhone,
                              enabled: !isLoading,
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              key: LoginPage.passwordFieldKey,
                              controller: _passwordController,
                              obscureText: _obscurePassword,
                              textInputAction: TextInputAction.done,
                              autofillHints: const [AutofillHints.password],
                              decoration: InputDecoration(
                                labelText: AppStrings.password,
                                prefixIcon: const Icon(
                                  Icons.lock,
                                  color: AppColors.primary,
                                ),
                                suffixIcon: IconButton(
                                  tooltip: _obscurePassword
                                      ? 'Hiện mật khẩu'
                                      : 'Ẩn mật khẩu',
                                  onPressed: () {
                                    setState(() {
                                      _obscurePassword = !_obscurePassword;
                                    });
                                  },
                                  icon: Icon(
                                    _obscurePassword
                                        ? Icons.visibility_off
                                        : Icons.visibility,
                                  ),
                                ),
                              ),
                              validator: _validatePassword,
                              enabled: !isLoading,
                              onFieldSubmitted: (_) {
                                if (!isLoading) _submit();
                              },
                            ),
                            const SizedBox(height: 4),
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton(
                                onPressed: isLoading
                                    ? null
                                    : () => context.pushNamed(
                                        AppRouteNames.resetPassword,
                                      ),
                                child: const Text(AppStrings.forgotPassword),
                              ),
                            ),
                            if (authState.errorMessage case final message?) ...[
                              const SizedBox(height: 4),
                              Semantics(
                                label: 'Thông báo đăng nhập',
                                liveRegion: true,
                                container: true,
                                child: Text(
                                  message,
                                  key: const ValueKey('login-error-message'),
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    color: Theme.of(context).colorScheme.error,
                                    fontSize: 13,
                                  ),
                                ),
                              ),
                            ],
                            const SizedBox(height: 12),
                            SizedBox(
                              width: double.infinity,
                              child: ElevatedButton(
                                onPressed: isLoading ? null : _submit,
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
                                    const Text(AppStrings.login),
                                  ],
                                ),
                              ),
                            ),
                            const Spacer(),
                            const SystemStatusIndicator(),
                            const Padding(
                              padding: EdgeInsets.only(top: 32, bottom: 12),
                              child: Column(
                                children: [
                                  Text(
                                    AppStrings.version,
                                    style: TextStyle(
                                      color: AppColors.mutedText,
                                      fontSize: 12,
                                    ),
                                  ),
                                  SizedBox(height: 4),
                                  Text(
                                    AppStrings.copyright,
                                    style: TextStyle(
                                      color: AppColors.mutedText,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}
