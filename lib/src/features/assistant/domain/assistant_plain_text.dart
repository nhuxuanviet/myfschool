String assistantPlainText(String value) {
  var plainText = value.replaceAllMapped(
    RegExp(r'\[([^\]]+)]\([^)]+\)'),
    (match) => match.group(1)!,
  );
  plainText = plainText.replaceAll(
    RegExp(r'^\s*#{1,6}\s*', multiLine: true),
    '',
  );
  plainText = plainText.replaceAll(
    RegExp(r'^\s*[-+]\s+', multiLine: true),
    '• ',
  );
  plainText = plainText
      .replaceAll('**', '')
      .replaceAll('__', '')
      .replaceAll('*', '')
      .replaceAll('`', '');
  return plainText.replaceAll(RegExp(r'\n{3,}'), '\n\n');
}
