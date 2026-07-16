enum HealthStatus {
  up,
  down,
  unknown;

  factory HealthStatus.fromApi(Object? value) {
    return switch (value?.toString().toUpperCase()) {
      'UP' => HealthStatus.up,
      'DOWN' => HealthStatus.down,
      _ => HealthStatus.unknown,
    };
  }
}

class HealthCheck {
  const HealthCheck({required this.status});

  factory HealthCheck.fromJson(Map<String, dynamic> json) {
    return HealthCheck(status: HealthStatus.fromApi(json['status']));
  }

  final HealthStatus status;
}
