import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/json_stream_client.dart';
import 'package:myfschoolse1913/src/features/assistant/data/assistant_api_repository.dart';

void main() {
  test('posts a normalized chat message', () async {
    final client = _FakeJsonStreamClient();
    final repository = ApiAssistantRepository(client);

    final events = await repository
        .streamChat('  Lịch học của em  ', conversationId: 'conversation-1')
        .toList();
    expect(client.path, ApiAssistantRepository.streamChatPath);
    expect(client.data, {
      'message': 'Lịch học của em',
      'conversationId': 'conversation-1',
    });
    expect(events.map((event) => event.content).join(), 'Thời khóa biểu');
    expect(events.last.type.name, 'done');
  });

  test('rejects invalid messages before the network call', () async {
    final repository = ApiAssistantRepository(_FakeJsonStreamClient());
    await expectLater(
      repository.streamChat('   ', conversationId: 'conversation-1'),
      emitsError(isArgumentError),
    );
    await expectLater(
      repository.streamChat('a' * 501, conversationId: 'conversation-1'),
      emitsError(isArgumentError),
    );
  });
}

final class _FakeJsonStreamClient implements JsonStreamClient {
  String? path;
  Map<String, dynamic>? data;

  @override
  Stream<List<int>> postJsonStream(
    String path, {
    required Map<String, dynamic> data,
  }) async* {
    this.path = path;
    this.data = data;
    final payload =
        '${jsonEncode({'type': 'delta', 'content': 'Thời khóa ', 'mode': 'LOCAL'})}\n'
        '${jsonEncode({'type': 'delta', 'content': 'biểu', 'mode': 'LOCAL'})}\n'
        '${jsonEncode({'type': 'done', 'content': '', 'mode': 'LOCAL'})}\n';
    yield utf8.encode(payload);
  }
}
