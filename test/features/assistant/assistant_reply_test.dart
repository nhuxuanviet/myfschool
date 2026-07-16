import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/assistant/domain/assistant_reply.dart';

void main() {
  test('parses a strict assistant reply', () {
    final reply = AssistantReply.fromJson({
      'answer': ' Thời khóa biểu của em. ',
      'mode': 'LOCAL',
    });
    expect(reply.answer, 'Thời khóa biểu của em.');
    expect(reply.mode, AssistantReplyMode.local);
  });

  test('rejects empty answers and unknown modes', () {
    expect(
      () => AssistantReply.fromJson({'answer': ' ', 'mode': 'LOCAL'}),
      throwsFormatException,
    );
    expect(
      () => AssistantReply.fromJson({'answer': 'OK', 'mode': 'UNKNOWN'}),
      throwsFormatException,
    );
  });
}
