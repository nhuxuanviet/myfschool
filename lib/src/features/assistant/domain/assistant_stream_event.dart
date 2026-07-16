import 'assistant_reply.dart';

enum AssistantStreamEventType { delta, done, error }

final class AssistantStreamEvent {
  const AssistantStreamEvent({
    required this.type,
    required this.content,
    required this.mode,
  });

  factory AssistantStreamEvent.fromJson(Map<String, dynamic> json) {
    final type = switch (json['type']) {
      'delta' => AssistantStreamEventType.delta,
      'done' => AssistantStreamEventType.done,
      'error' => AssistantStreamEventType.error,
      _ => throw const FormatException('Unsupported assistant stream event.'),
    };
    final content = json['content'];
    final mode = json['mode'];
    if (content is! String || mode is! String) {
      throw const FormatException('Invalid assistant stream event.');
    }
    return AssistantStreamEvent(
      type: type,
      content: content,
      mode: AssistantReplyMode.fromJson(mode),
    );
  }

  final AssistantStreamEventType type;
  final String content;
  final AssistantReplyMode mode;
}
