import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/clubs/data/clubs_api_repository.dart';
import 'package:myfschoolse1913/src/features/clubs/domain/school_club.dart';

import '../../helpers/test_clubs.dart';

void main() {
  test('uses list, detail, apply, and withdraw contracts', () async {
    final requests = <RequestOptions>[];
    var index = 0;
    final responses = [
      testClubsFeedJson(),
      testClubJson(),
      testClubJson(
        membershipStatus: 'PENDING',
        canApply: false,
        canWithdraw: true,
      ),
      testClubJson(membershipStatus: 'WITHDRAWN', canApply: true),
    ];
    final dio = Dio()
      ..interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            requests.add(options);
            handler.resolve(
              Response<Object?>(
                requestOptions: options,
                statusCode: 200,
                data: responses[index++],
              ),
            );
          },
        ),
      );
    final repository = ApiClubsRepository(ApiClient(dio));

    await repository.getClubs(category: ClubCategory.academic);
    await repository.getClub(testOpenClubId);
    await repository.apply(testOpenClubId);
    await repository.withdraw(testOpenClubId);

    expect(requests.map((request) => request.method), [
      'GET',
      'GET',
      'POST',
      'DELETE',
    ]);
    expect(requests.first.queryParameters, {'category': 'ACADEMIC'});
    expect(requests.last.path, '/api/v1/clubs/$testOpenClubId/applications');
  });
}
