import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/assistant_api_repository.dart';
import '../domain/assistant_plain_text.dart';
import '../domain/assistant_reply.dart';
import '../domain/assistant_repository.dart';
import '../domain/assistant_stream_event.dart';

final assistantRepositoryProvider = Provider<AssistantRepository>(
  (ref) =>
      ApiAssistantRepository(ref.watch(authenticatedJsonStreamClientProvider)),
);

enum AssistantMessageRole { student, assistant }

final class AssistantMessage {
  const AssistantMessage({
    required this.role,
    required this.content,
    this.isStreaming = false,
  });
  final AssistantMessageRole role;
  final String content;
  final bool isStreaming;
}

final class AssistantState {
  const AssistantState({
    required this.messages,
    this.isSubmitting = false,
    this.errorMessage,
  });

  final List<AssistantMessage> messages;
  final bool isSubmitting;
  final String? errorMessage;
}

final assistantControllerProvider =
    NotifierProvider<AssistantController, AssistantState>(
      AssistantController.new,
    );

class AssistantController extends Notifier<AssistantState> {
  String _conversationId = _newConversationId();

  static const _welcome = AssistantMessage(
    role: AssistantMessageRole.assistant,
    content:
        'Chào em! Mình có thể giúp tra cứu lịch học, điểm, sự kiện, đơn từ, CLB và thông báo.',
  );

  @override
  AssistantState build() => const AssistantState(messages: [_welcome]);

  void clearConversation() {
    if (state.isSubmitting) return;
    _conversationId = _newConversationId();
    state = const AssistantState(messages: [_welcome]);
  }

  Future<bool> send(String message) async {
    final normalized = message.trim();
    if (state.isSubmitting || normalized.isEmpty || normalized.length > 500) {
      return false;
    }
    final submittedMessages = [
      ...state.messages,
      AssistantMessage(role: AssistantMessageRole.student, content: normalized),
    ];
    state = AssistantState(
      messages: [
        ...submittedMessages,
        const AssistantMessage(
          role: AssistantMessageRole.assistant,
          content: '',
          isStreaming: true,
        ),
      ],
      isSubmitting: true,
    );
    try {
      var rawAnswer = '';
      await for (final event
          in ref
              .read(assistantRepositoryProvider)
              .streamChat(normalized, conversationId: _conversationId)) {
        if (!ref.mounted) {
          return false;
        }
        if (event.type == AssistantStreamEventType.delta) {
          rawAnswer += event.content;
          state = AssistantState(
            messages: [
              ...submittedMessages,
              AssistantMessage(
                role: AssistantMessageRole.assistant,
                content: assistantPlainText(rawAnswer),
                isStreaming: true,
              ),
            ],
            isSubmitting: true,
          );
          await Future<void>.delayed(
            event.mode == AssistantReplyMode.local
                ? const Duration(milliseconds: 45)
                : const Duration(milliseconds: 12),
          );
        } else if (event.type == AssistantStreamEventType.error) {
          throw const FormatException('The assistant stream failed.');
        }
      }
      if (!ref.mounted) {
        return false;
      }
      final answer = assistantPlainText(rawAnswer).trim();
      if (answer.isEmpty) {
        throw const FormatException('The assistant returned no content.');
      }
      state = AssistantState(
        messages: [
          ...submittedMessages,
          AssistantMessage(
            role: AssistantMessageRole.assistant,
            content: answer,
          ),
        ],
      );
      return true;
    } on Object {
      if (!ref.mounted) {
        return false;
      }
      state = AssistantState(
        messages: submittedMessages,
        errorMessage: 'Không thể nhận câu trả lời lúc này. Vui lòng thử lại.',
      );
      return false;
    }
  }

  static String _newConversationId() =>
      'student-${DateTime.now().microsecondsSinceEpoch.toRadixString(36)}';
}
