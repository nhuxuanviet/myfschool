import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/assistant/application/assistant_providers.dart';
import 'package:myfschoolse1913/src/features/assistant/domain/assistant_repository.dart';
import 'package:myfschoolse1913/src/features/assistant/domain/assistant_reply.dart';
import 'package:myfschoolse1913/src/features/assistant/domain/assistant_stream_event.dart';
import 'package:myfschoolse1913/src/features/assistant/presentation/assistant_page.dart';

void main() {
  testWidgets('sends a question and renders the answer', (tester) async {
    final repository = _FakeAssistantRepository();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [assistantRepositoryProvider.overrideWithValue(repository)],
        child: const MaterialApp(home: AssistantPage()),
      ),
    );

    expect(find.bySemanticsLabel('Trợ lý học sinh'), findsOneWidget);
    await tester.enterText(find.bySemanticsLabel('Câu hỏi'), 'Lịch học của em');
    await tester.tap(find.bySemanticsLabel('Gửi câu hỏi'));
    await tester.pump();

    expect(find.bySemanticsLabel('Trợ lý đang trả lời'), findsOneWidget);
    expect(find.byType(Image), findsNWidgets(2));
    await tester.pumpAndSettle();

    expect(repository.messages, ['Lịch học của em']);
    expect(
      find.bySemanticsLabel('Trợ lý trả lời: Thời khóa biểu tuần này'),
      findsOneWidget,
    );
    expect(find.textContaining('**'), findsNothing);
  });
}

final class _FakeAssistantRepository implements AssistantRepository {
  final List<String> messages = [];

  @override
  Stream<AssistantStreamEvent> streamChat(
    String message, {
    required String conversationId,
  }) async* {
    messages.add(message);
    await Future<void>.delayed(const Duration(milliseconds: 20));
    yield const AssistantStreamEvent(
      type: AssistantStreamEventType.delta,
      content: '**Thời khóa biểu** ',
      mode: AssistantReplyMode.local,
    );
    yield const AssistantStreamEvent(
      type: AssistantStreamEventType.delta,
      content: 'tuần này',
      mode: AssistantReplyMode.local,
    );
    yield const AssistantStreamEvent(
      type: AssistantStreamEventType.done,
      content: '',
      mode: AssistantReplyMode.local,
    );
  }
}
