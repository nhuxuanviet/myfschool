import 'assistant_stream_event.dart';

abstract interface class AssistantRepository {
  Stream<AssistantStreamEvent> streamChat(
    String message, {
    required String conversationId,
  });
}
