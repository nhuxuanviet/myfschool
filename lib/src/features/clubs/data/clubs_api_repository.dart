import '../../../core/network/api_client.dart';
import '../domain/clubs_repository.dart';
import '../domain/school_club.dart';

final class ApiClubsRepository implements ClubsRepository {
  const ApiClubsRepository(this._apiClient);
  static const clubsPath = '/api/v1/clubs';
  final ApiClient _apiClient;

  @override
  Future<ClubsFeed> getClubs({ClubCategory? category}) async =>
      ClubsFeed.fromJson(
        await _apiClient.getJson(
          clubsPath,
          queryParameters: category == null
              ? null
              : {'category': category.apiValue},
        ),
      );

  @override
  Future<SchoolClub> getClub(String clubId) async =>
      SchoolClub.fromJson(await _apiClient.getJson(_clubPath(clubId)));

  @override
  Future<SchoolClub> apply(String clubId) async => SchoolClub.fromJson(
    await _apiClient.postJson('${_clubPath(clubId)}/applications'),
  );

  @override
  Future<SchoolClub> withdraw(String clubId) async => SchoolClub.fromJson(
    await _apiClient.deleteJson('${_clubPath(clubId)}/applications'),
  );

  static String _clubPath(String clubId) {
    final id = canonicalClubId(clubId);
    if (id == null) {
      throw ArgumentError.value(clubId, 'clubId', 'must be a UUID');
    }
    return '$clubsPath/${Uri.encodeComponent(id)}';
  }
}
