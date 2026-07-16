import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/assistant/domain/assistant_plain_text.dart';

void main() {
  test('removes common Markdown syntax from assistant output', () {
    const markdown = '''
## Lịch học
- **Thứ Hai:** [Toán](https://example.test)
- `Ngữ văn`
''';

    expect(
      assistantPlainText(markdown).trim(),
      'Lịch học\n• Thứ Hai: Toán\n• Ngữ văn',
    );
  });
}
