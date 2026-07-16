abstract interface class JsonStreamClient {
  Stream<List<int>> postJsonStream(
    String path, {
    required Map<String, dynamic> data,
  });
}
