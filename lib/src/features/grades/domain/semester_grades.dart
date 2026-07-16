/// Semester grades returned by the authenticated student API.
///
/// Calendar dates are represented at UTC midnight because they are school
/// dates, rather than instants in the supplied [timeZone].
final class SemesterGrades {
  const SemesterGrades({
    required this.timeZone,
    required this.selectedTerm,
    required this.availableTerms,
    required this.subjects,
  });

  factory SemesterGrades.fromJson(Map<String, dynamic> json) {
    final selectedTerm = GradeTerm.fromJson(_requiredMap(json, 'selectedTerm'));
    final availableTerms = _requiredList(json, 'availableTerms')
        .map((value) {
          if (value is! Map) {
            throw const FormatException('Invalid available academic term.');
          }
          return GradeTerm.fromJson(Map<String, dynamic>.from(value));
        })
        .toList(growable: false);
    final subjects = _requiredList(json, 'subjects')
        .map((value) {
          if (value is! Map) {
            throw const FormatException('Invalid grade subject.');
          }
          return GradeSubject.fromJson(Map<String, dynamic>.from(value));
        })
        .toList(growable: false);

    if (availableTerms.isEmpty) {
      throw const FormatException('At least one academic term is required.');
    }
    final selectedTermIndex = availableTerms.indexWhere(
      (term) => term.id == selectedTerm.id,
    );
    if (selectedTermIndex < 0) {
      throw const FormatException(
        'The selected academic term must be available to the student.',
      );
    }
    if (!_isSameTerm(selectedTerm, availableTerms[selectedTermIndex])) {
      throw const FormatException(
        'The selected academic term must match its available-term record.',
      );
    }
    _validateUniqueTermIds(availableTerms);
    _validateNewestFirst(availableTerms);
    _validateUniqueSubjectCodes(subjects);

    return SemesterGrades(
      timeZone: _requiredString(json, 'timeZone'),
      selectedTerm: selectedTerm,
      availableTerms: availableTerms,
      subjects: subjects,
    );
  }

  final String timeZone;
  final GradeTerm selectedTerm;
  final List<GradeTerm> availableTerms;
  final List<GradeSubject> subjects;
}

final class GradeTerm {
  const GradeTerm({
    required this.id,
    required this.academicYear,
    required this.code,
    required this.name,
    required this.startsOn,
    required this.endsOn,
  });

  factory GradeTerm.fromJson(Map<String, dynamic> json) {
    final startsOn = _requiredIsoDate(json, 'startsOn');
    final endsOn = _requiredIsoDate(json, 'endsOn');
    if (startsOn.isAfter(endsOn)) {
      throw const FormatException('Academic term dates are invalid.');
    }

    return GradeTerm(
      id: _requiredUuid(json, 'id'),
      academicYear: _requiredString(json, 'academicYear'),
      code: _requiredString(json, 'code'),
      name: _requiredString(json, 'name'),
      startsOn: startsOn,
      endsOn: endsOn,
    );
  }

  final String id;
  final String academicYear;
  final String code;
  final String name;
  final DateTime startsOn;
  final DateTime endsOn;
}

enum AssessmentMode {
  numeric('NUMERIC'),
  remark('REMARK');

  const AssessmentMode(this.apiValue);

  final String apiValue;

  static AssessmentMode fromJson(String value) {
    for (final mode in values) {
      if (mode.apiValue == value) return mode;
    }
    throw FormatException('Invalid assessmentMode: $value.');
  }
}

enum TermResult {
  achieved('ACHIEVED', 'Đạt'),
  notAchieved('NOT_ACHIEVED', 'Chưa đạt'),
  pending('PENDING', 'Chờ đánh giá');

  const TermResult(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static TermResult fromJson(String value) {
    for (final result in values) {
      if (result.apiValue == value) return result;
    }
    throw FormatException('Invalid termResult: $value.');
  }
}

final class GradeSubject {
  const GradeSubject({
    required this.code,
    required this.name,
    required this.assessmentMode,
    required this.annualLessonCount,
    required this.requiredRegularAssessments,
    required this.termAverage,
    required this.termResult,
    required this.assessments,
  });

  factory GradeSubject.fromJson(Map<String, dynamic> json) {
    final assessmentMode = AssessmentMode.fromJson(
      _requiredString(json, 'assessmentMode'),
    );
    final annualLessonCount = _requiredNullableNonNegativeInt(
      json,
      'annualLessonCount',
    );
    final requiredRegularAssessments = _requiredPositiveInt(
      json,
      'requiredRegularAssessments',
    );
    final termAverage = _requiredNullableScore(json, 'termAverage');
    final termResult = _requiredNullableTermResult(json, 'termResult');
    final assessments = _requiredList(json, 'assessments')
        .map((value) {
          if (value is! Map) {
            throw const FormatException('Invalid grade assessment.');
          }
          return GradeAssessment.fromJson(
            Map<String, dynamic>.from(value),
            assessmentMode: assessmentMode,
          );
        })
        .toList(growable: false);

    _validateSubjectConfiguration(
      assessmentMode: assessmentMode,
      annualLessonCount: annualLessonCount,
      requiredRegularAssessments: requiredRegularAssessments,
      termAverage: termAverage,
      termResult: termResult,
    );

    return GradeSubject(
      code: _requiredString(json, 'code'),
      name: _requiredString(json, 'name'),
      assessmentMode: assessmentMode,
      annualLessonCount: annualLessonCount,
      requiredRegularAssessments: requiredRegularAssessments,
      termAverage: termAverage,
      termResult: termResult,
      assessments: assessments,
    );
  }

  final String code;
  final String name;
  final AssessmentMode assessmentMode;
  final int? annualLessonCount;
  final int requiredRegularAssessments;
  final double? termAverage;
  final TermResult? termResult;
  final List<GradeAssessment> assessments;
}

enum AssessmentKind {
  regular('REGULAR', 'Đánh giá thường xuyên'),
  midterm('MIDTERM', 'Giữa kỳ'),
  finalExam('FINAL', 'Cuối kỳ');

  const AssessmentKind(this.apiValue, this.sectionLabel);

  final String apiValue;
  final String sectionLabel;

  static AssessmentKind fromJson(String value) {
    for (final kind in values) {
      if (kind.apiValue == value) return kind;
    }
    throw FormatException('Invalid assessment kind: $value.');
  }
}

enum AssessmentForm {
  oral('ORAL', 'Miệng'),
  written('WRITTEN', 'Viết'),
  presentation('PRESENTATION', 'Thuyết trình'),
  practical('PRACTICAL', 'Thực hành'),
  experiment('EXPERIMENT', 'Thí nghiệm'),
  product('PRODUCT', 'Sản phẩm'),
  project('PROJECT', 'Dự án');

  const AssessmentForm(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static AssessmentForm fromJson(String value) {
    for (final form in values) {
      if (form.apiValue == value) return form;
    }
    throw FormatException('Invalid assessment form: $value.');
  }
}

enum AssessmentStatus {
  recorded('RECORDED', 'Đã ghi nhận'),
  makeUpRequired('MAKE_UP_REQUIRED', 'Cần kiểm tra bù'),
  excused('EXCUSED', 'Được miễn'),
  absentFinalized('ABSENT_FINALIZED', 'Vắng đã chốt');

  const AssessmentStatus(this.apiValue, this.label);

  final String apiValue;
  final String label;

  bool get isFinalized =>
      this == AssessmentStatus.recorded ||
      this == AssessmentStatus.absentFinalized;

  static AssessmentStatus fromJson(String value) {
    for (final status in values) {
      if (status.apiValue == value) return status;
    }
    throw FormatException('Invalid assessment status: $value.');
  }
}

enum AssessmentOutcome {
  achieved('ACHIEVED', 'Đạt'),
  notAchieved('NOT_ACHIEVED', 'Chưa đạt');

  const AssessmentOutcome(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static AssessmentOutcome fromJson(String value) {
    for (final outcome in values) {
      if (outcome.apiValue == value) return outcome;
    }
    throw FormatException('Invalid assessment outcome: $value.');
  }
}

final class GradeAssessment {
  const GradeAssessment({
    required this.kind,
    required this.form,
    required this.displayLabel,
    required this.durationMinutes,
    required this.status,
    required this.score,
    required this.outcome,
    required this.assessedOn,
  });

  factory GradeAssessment.fromJson(
    Map<String, dynamic> json, {
    required AssessmentMode assessmentMode,
  }) {
    final status = AssessmentStatus.fromJson(_requiredString(json, 'status'));
    final score = _requiredNullableScore(json, 'score');
    final outcome = _requiredNullableOutcome(json, 'outcome');
    _validateAssessmentResult(
      assessmentMode: assessmentMode,
      status: status,
      score: score,
      outcome: outcome,
    );

    return GradeAssessment(
      kind: AssessmentKind.fromJson(_requiredString(json, 'kind')),
      form: AssessmentForm.fromJson(_requiredString(json, 'form')),
      displayLabel: _requiredString(json, 'displayLabel'),
      durationMinutes: _requiredNullablePositiveInt(json, 'durationMinutes'),
      status: status,
      score: score,
      outcome: outcome,
      assessedOn: _requiredNullableIsoDate(json, 'assessedOn'),
    );
  }

  final AssessmentKind kind;
  final AssessmentForm form;
  final String displayLabel;
  final int? durationMinutes;
  final AssessmentStatus status;
  final double? score;
  final AssessmentOutcome? outcome;
  final DateTime? assessedOn;

  String get label => displayLabel;
}

String formatGradeDate(DateTime date) {
  String twoDigits(int value) => value.toString().padLeft(2, '0');
  return '${date.year.toString().padLeft(4, '0')}-${twoDigits(date.month)}-'
      '${twoDigits(date.day)}';
}

Map<String, dynamic> _requiredMap(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! Map) {
    throw FormatException('Missing required object: $key.');
  }
  return Map<String, dynamic>.from(value);
}

List<Object?> _requiredList(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! List) {
    throw FormatException('Missing required list: $key.');
  }
  return List<Object?>.from(value);
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Missing required string: $key.');
  }
  return value.trim();
}

String _requiredUuid(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  if (!isValidGradeTermId(value)) {
    throw FormatException('Invalid UUID: $key.');
  }
  return value;
}

/// Returns whether [value] is a UUID accepted by the grades API for a term.
bool isValidGradeTermId(String? value) {
  return value != null &&
      RegExp(
        r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
      ).hasMatch(value);
}

int _requiredPositiveInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value <= 0) {
    throw FormatException('Invalid positive integer: $key.');
  }
  return value;
}

int? _requiredNullableNonNegativeInt(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! int || value < 0) {
    throw FormatException('Invalid non-negative integer: $key.');
  }
  return value;
}

int? _requiredNullablePositiveInt(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! int || value <= 0 || value > 180) {
    throw FormatException('Invalid assessment duration: $key.');
  }
  return value;
}

double? _requiredNullableScore(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! num ||
      !value.isFinite ||
      value < 0 ||
      value > 10 ||
      !_hasAtMostOneDecimalPlace(value)) {
    throw FormatException('Invalid score: $key.');
  }
  return value.toDouble();
}

bool _hasAtMostOneDecimalPlace(num value) {
  final tenths = value.toDouble() * 10;
  return (tenths - tenths.round()).abs() < 0.000000001;
}

TermResult? _requiredNullableTermResult(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! String) {
    throw FormatException('Invalid nullable term result: $key.');
  }
  return TermResult.fromJson(value);
}

AssessmentOutcome? _requiredNullableOutcome(
  Map<String, dynamic> json,
  String key,
) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! String) {
    throw FormatException('Invalid nullable assessment outcome: $key.');
  }
  return AssessmentOutcome.fromJson(value);
}

DateTime _requiredIsoDate(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  return _parseIsoDate(value, key);
}

DateTime? _requiredNullableIsoDate(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Invalid nullable date: $key.');
  }
  return _parseIsoDate(value.trim(), key);
}

DateTime _parseIsoDate(String value, String key) {
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(value);
  if (match == null) {
    throw FormatException('Invalid date: $key.');
  }
  final date = DateTime.utc(
    int.parse(match.group(1)!),
    int.parse(match.group(2)!),
    int.parse(match.group(3)!),
  );
  if (formatGradeDate(date) != value) {
    throw FormatException('Invalid date: $key.');
  }
  return date;
}

void _validateAssessmentResult({
  required AssessmentMode assessmentMode,
  required AssessmentStatus status,
  required double? score,
  required AssessmentOutcome? outcome,
}) {
  if (!status.isFinalized) {
    if (score != null || outcome != null) {
      throw const FormatException(
        'Unfinalized assessments must not expose a result.',
      );
    }
    return;
  }

  switch (assessmentMode) {
    case AssessmentMode.numeric:
      if (score == null || outcome != null) {
        throw const FormatException(
          'Finalized numeric assessments require only a score.',
        );
      }
    case AssessmentMode.remark:
      if (score != null || outcome == null) {
        throw const FormatException(
          'Finalized remark assessments require only an outcome.',
        );
      }
  }
}

void _validateSubjectConfiguration({
  required AssessmentMode assessmentMode,
  required int? annualLessonCount,
  required int requiredRegularAssessments,
  required double? termAverage,
  required TermResult? termResult,
}) {
  switch (assessmentMode) {
    case AssessmentMode.numeric:
      if (annualLessonCount == null || annualLessonCount < 35) {
        throw const FormatException(
          'Numeric subjects require at least 35 annual lessons.',
        );
      }
      if (termResult != null) {
        throw const FormatException(
          'Numeric subjects must not expose a remark term result.',
        );
      }
      final expectedRegularAssessments = switch (annualLessonCount) {
        <= 35 => 2,
        <= 70 => 3,
        _ => 4,
      };
      if (requiredRegularAssessments != expectedRegularAssessments) {
        throw const FormatException(
          'Numeric subject regular-assessment requirement is invalid.',
        );
      }
    case AssessmentMode.remark:
      if (annualLessonCount != null) {
        throw const FormatException(
          'Remark subjects must not expose an annual lesson count.',
        );
      }
      if (termAverage != null) {
        throw const FormatException(
          'Remark subjects must not expose a numeric term average.',
        );
      }
      if (termResult == null) {
        throw const FormatException('Remark subjects require a term result.');
      }
      if (requiredRegularAssessments != 2) {
        throw const FormatException(
          'Remark subjects require two regular assessments.',
        );
      }
  }
}

void _validateUniqueTermIds(List<GradeTerm> terms) {
  final ids = <String>{};
  for (final term in terms) {
    if (!ids.add(term.id)) {
      throw const FormatException('Academic term IDs must be unique.');
    }
  }
}

void _validateNewestFirst(List<GradeTerm> terms) {
  for (var index = 1; index < terms.length; index += 1) {
    if (_compareTermRecency(terms[index - 1], terms[index]) < 0) {
      throw const FormatException('Academic terms must be newest first.');
    }
  }
}

int _compareTermRecency(GradeTerm left, GradeTerm right) {
  final startComparison = left.startsOn.compareTo(right.startsOn);
  if (startComparison != 0) return startComparison;
  final endComparison = left.endsOn.compareTo(right.endsOn);
  if (endComparison != 0) return endComparison;
  final yearComparison = left.academicYear.compareTo(right.academicYear);
  if (yearComparison != 0) return yearComparison;
  return left.code.compareTo(right.code);
}

bool _isSameTerm(GradeTerm left, GradeTerm right) {
  return left.id == right.id &&
      left.academicYear == right.academicYear &&
      left.code == right.code &&
      left.name == right.name &&
      left.startsOn == right.startsOn &&
      left.endsOn == right.endsOn;
}

void _validateUniqueSubjectCodes(List<GradeSubject> subjects) {
  final subjectCodes = <String>{};
  for (final subject in subjects) {
    if (!subjectCodes.add(subject.code)) {
      throw const FormatException('Subject codes must be unique.');
    }
  }
}
