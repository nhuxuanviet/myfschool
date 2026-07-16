import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app/app_router.dart';
import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_dimensions.dart';
import '../application/assistant_providers.dart';

class AssistantPage extends ConsumerStatefulWidget {
  const AssistantPage({this.embedded = false, super.key});

  final bool embedded;

  @override
  ConsumerState<AssistantPage> createState() => _AssistantPageState();
}

class _AssistantPageState extends ConsumerState<AssistantPage> {
  final _controller = TextEditingController();
  final _scrollController = ScrollController();

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(assistantControllerProvider);
    ref.listen(assistantControllerProvider, (previous, next) {
      final previousLast = previous?.messages.lastOrNull;
      final nextLast = next.messages.lastOrNull;
      final messageAdded = previous?.messages.length != next.messages.length;
      final shouldFollow = messageAdded || _isNearBottom();
      if (previous?.messages.length != next.messages.length ||
          previousLast?.content != nextLast?.content) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (shouldFollow) _scrollToEnd();
        });
      }
    });
    return Semantics(
      label: 'Trợ lý học sinh',
      container: true,
      explicitChildNodes: true,
      child: Scaffold(
        backgroundColor: AppColors.surface,
        appBar: AppBar(
          toolbarHeight: 72,
          leading: IconButton(
            tooltip: widget.embedded ? 'Đóng trợ lý' : 'Quay lại',
            onPressed: widget.embedded
                ? () => Navigator.pop(context)
                : () => context.go(AppRoutes.home),
            icon: Icon(widget.embedded ? Icons.close : Icons.arrow_back),
          ),
          title: const ExcludeSemantics(
            child: Column(
              children: [
                Text('Trợ lý AI'),
                SizedBox(height: 2),
                Text(
                  'FPT SCHOOLS AI',
                  style: TextStyle(
                    color: Color(0xFF9A9EAB),
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.2,
                  ),
                ),
              ],
            ),
          ),
          actions: [
            IconButton(
              tooltip: 'Tùy chọn trợ lý',
              onPressed: () => _showOptions(context),
              icon: const Icon(Icons.more_horiz),
            ),
            const SizedBox(width: 10),
          ],
        ),
        body: SafeArea(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                maxWidth: AppDimensions.contentMaxWidth,
              ),
              child: Column(
                children: [
                  Expanded(
                    child: Scrollbar(
                      controller: _scrollController,
                      thumbVisibility: state.messages.length > 3,
                      child: ListView.separated(
                        controller: _scrollController,
                        physics: const ClampingScrollPhysics(),
                        keyboardDismissBehavior:
                            ScrollViewKeyboardDismissBehavior.onDrag,
                        padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
                        itemCount: state.messages.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 20),
                        itemBuilder: (context, index) =>
                            _MessageBubble(message: state.messages[index]),
                      ),
                    ),
                  ),
                  if (state.errorMessage case final error?)
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Text(
                        error,
                        style: const TextStyle(color: Color(0xFFC62828)),
                      ),
                    ),
                  _Composer(
                    controller: _controller,
                    isSubmitting: state.isSubmitting,
                    onSend: _send,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _showOptions(BuildContext context) async {
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: ListTile(
          leading: const Icon(Icons.delete_outline),
          title: const Text('Xóa cuộc trò chuyện'),
          onTap: () {
            ref.read(assistantControllerProvider.notifier).clearConversation();
            Navigator.pop(sheetContext);
          },
        ),
      ),
    );
  }

  Future<void> _send() async {
    final message = _controller.text;
    if (message.trim().isEmpty) {
      return;
    }
    _controller.clear();
    await ref.read(assistantControllerProvider.notifier).send(message);
  }

  void _scrollToEnd() {
    if (!mounted || !_scrollController.hasClients) {
      return;
    }
    _scrollController.animateTo(
      _scrollController.position.maxScrollExtent,
      duration: const Duration(milliseconds: 200),
      curve: Curves.easeOut,
    );
  }

  bool _isNearBottom() {
    if (!_scrollController.hasClients) return true;
    return _scrollController.position.extentAfter < 96;
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message});
  final AssistantMessage message;

  @override
  Widget build(BuildContext context) {
    final isStudent = message.role == AssistantMessageRole.student;
    final bubble = Container(
      constraints: const BoxConstraints(maxWidth: 310),
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
      decoration: BoxDecoration(
        color: isStudent ? const Color(0xFFFFE7DC) : const Color(0xFFF5F3FB),
        borderRadius: BorderRadius.circular(22),
      ),
      child: message.content.isEmpty && message.isStreaming
          ? const SizedBox.square(
              dimension: 18,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          : Text.rich(
              TextSpan(
                text: message.content,
                children: [
                  if (message.isStreaming)
                    const TextSpan(
                      text: ' ▍',
                      style: TextStyle(color: AppColors.primary),
                    ),
                ],
              ),
              style: const TextStyle(
                color: AppColors.text,
                fontSize: 16,
                height: 1.45,
              ),
            ),
    );
    return Semantics(
      label: message.content.isEmpty && message.isStreaming
          ? 'Trợ lý đang trả lời'
          : '${isStudent ? 'Em hỏi' : 'Trợ lý trả lời'}: ${message.content}',
      container: true,
      excludeSemantics: true,
      child: Align(
        alignment: isStudent ? Alignment.centerRight : Alignment.centerLeft,
        child: isStudent
            ? bubble
            : Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(14),
                    child: Image.asset(
                      AppAssets.aiRobot,
                      width: 54,
                      height: 54,
                      fit: BoxFit.cover,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Flexible(child: bubble),
                ],
              ),
      ),
    );
  }
}

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.isSubmitting,
    required this.onSend,
  });

  final TextEditingController controller;
  final bool isSubmitting;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surface,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 18),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: controller,
                  enabled: !isSubmitting,
                  maxLength: 500,
                  minLines: 1,
                  maxLines: 4,
                  textInputAction: TextInputAction.newline,
                  decoration: const InputDecoration(
                    labelText: 'Câu hỏi',
                    hintText: 'Nhắn tin cho trợ lý AI...',
                    counterText: '',
                    filled: false,
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(6),
                child: Semantics(
                  label: 'Gửi câu hỏi',
                  button: true,
                  enabled: !isSubmitting,
                  excludeSemantics: true,
                  child: IconButton.filled(
                    onPressed: isSubmitting ? null : onSend,
                    icon: isSubmitting
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Icon(Icons.arrow_forward),
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
