import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/clubs/domain/school_club.dart';

import '../../helpers/test_clubs.dart';

void main() {
  test('parses club feed and membership actions', () {
    final feed = ClubsFeed.fromJson(testClubsFeedJson());
    expect(feed.clubs, hasLength(2));
    expect(feed.clubs.first.canApply, isTrue);
    expect(feed.clubs.last.membershipStatus, ClubMembershipStatus.pending);
    expect(feed.clubs.last.canWithdraw, isTrue);
  });

  test('rejects invalid capacity and action combinations', () {
    expect(
      () =>
          SchoolClub.fromJson(testClubJson(capacity: 5, activeMemberCount: 6)),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => SchoolClub.fromJson(testClubJson(canWithdraw: true)),
      throwsA(isA<FormatException>()),
    );
  });

  test('canonicalizes club IDs and uses Vietnam time', () {
    expect(canonicalClubId(testOpenClubId.toUpperCase()), testOpenClubId);
    expect(
      formatClubInstant(DateTime.parse('2026-07-11T18:30:00Z')),
      '01:30 • 12/07/2026',
    );
  });
}
