import 'dart:convert';

import '../../../core/network/json_stream_client.dart';
import '../domain/assistant_repository.dart';
import '../domain/assistant_stream_event.dart';

final class ApiAssistantRepository implements AssistantRepository {
  const ApiAssistantRepository(this._streamClient);

  static const streamChatPath = '/api/v1/assistant/chat/stream';
  final JsonStreamClient _streamClient;

  @override
  Stream<AssistantStreamEvent> streamChat(
    String message, {
    required String conversationId,
  }) async* {
    final normalized = message.trim();
    if (normalized.isEmpty || normalized.length > 500) {
      throw ArgumentError.value(
        message,
        'message',
        'must contain 1 to 500 characters',
      );
    }
    final lines = _streamClient
        .postJsonStream(
          streamChatPath,
          data: {'message': normalized, 'conversationId': conversationId},
        )
        .transform(utf8.decoder)
        .transform(const LineSplitter());
    await for (final line in lines) {
      if (line.trim().isEmpty) {
        continue;
      }
      final decoded = jsonDecode(line);
      if (decoded is! Map) {
        throw const FormatException('Invalid assistant stream payload.');
      }
      yield AssistantStreamEvent.fromJson(Map<String, dynamic>.from(decoded));
    }
  }
}
