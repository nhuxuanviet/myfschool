import 'dart:convert';

abstract final class AuthInputPolicy {
  static final RegExp _vietnameseMobile = RegExp(r'^(?:0|84)[35789][0-9]{8}$');
  static final RegExp _asciiUppercase = RegExp('[A-Z]');
  static final RegExp _asciiLowercase = RegExp('[a-z]');
  static final RegExp _asciiDigit = RegExp('[0-9]');
  static final RegExp _whitespace = RegExp(r'\s');

  static bool isVietnameseMobile(String value) {
    return _vietnameseMobile.hasMatch(value.trim());
  }

  static bool isStrongPassword(String value) {
    return value.length >= 8 &&
        utf8.encode(value).length <= 72 &&
        !_whitespace.hasMatch(value) &&
        _asciiUppercase.hasMatch(value) &&
        _asciiLowercase.hasMatch(value) &&
        _asciiDigit.hasMatch(value) &&
        value.runes.any(_isAsciiSpecialCharacter);
  }

  static bool _isAsciiSpecialCharacter(int rune) {
    return (rune >= 33 && rune <= 47) ||
        (rune >= 58 && rune <= 64) ||
        (rune >= 91 && rune <= 96) ||
        (rune >= 123 && rune <= 126);
  }
}
