enum AssistantReplyMode {
  local('LOCAL'),
  openAi('OPENAI');

  const AssistantReplyMode(this.apiValue);
  final String apiValue;

  static AssistantReplyMode fromJson(Object? value) {
    if (value is! String) {
      throw const FormatException('Missing assistant reply mode.');
    }
    return values.firstWhere(
      (mode) => mode.apiValue == value,
      orElse: () =>
          throw FormatException('Invalid assistant reply mode: $value.'),
    );
  }
}

final class AssistantReply {
  const AssistantReply({required this.answer, required this.mode});

  factory AssistantReply.fromJson(Map<String, dynamic> json) {
    final answer = json['answer'];
    if (answer is! String || answer.trim().isEmpty) {
      throw const FormatException('Missing assistant answer.');
    }
    return AssistantReply(
      answer: answer.trim(),
      mode: AssistantReplyMode.fromJson(json['mode']),
    );
  }

  final String answer;
  final AssistantReplyMode mode;
}
