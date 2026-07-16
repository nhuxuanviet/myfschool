import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/clubs/application/clubs_providers.dart';
import 'package:myfschoolse1913/src/features/clubs/domain/clubs_repository.dart';
import 'package:myfschoolse1913/src/features/clubs/domain/school_club.dart';

import '../../helpers/test_clubs.dart';

void main() {
  testWidgets('lists clubs and opens details', (tester) async {
    final repository = _FakeClubsRepository();
    final router = await _pump(tester, repository);
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel('Câu lạc bộ'), findsWidgets);
    expect(repository.categories, [null]);

    await tester.tap(find.bySemanticsLabel('Xem CLB $testOpenClubId'));
    await tester.pumpAndSettle();
    expect(router.state.uri.path, '/clubs/$testOpenClubId');
    expect(
      find.bySemanticsLabel('Chi tiết CLB $testOpenClubId'),
      findsOneWidget,
    );
  });

  testWidgets('applies then withdraws a club application', (tester) async {
    final repository = _FakeClubsRepository();
    final router = await _pump(
      tester,
      repository,
      initialLocation: '/clubs/$testOpenClubId',
    );
    addTearDown(router.dispose);

    await tester.ensureVisible(find.bySemanticsLabel('Đăng ký CLB'));
    await tester.tap(find.bySemanticsLabel('Đăng ký CLB'));
    await tester.pumpAndSettle();
    expect(repository.applied, [testOpenClubId]);
    expect(find.bySemanticsLabel('Rút đơn CLB'), findsOneWidget);

    await tester.ensureVisible(find.bySemanticsLabel('Rút đơn CLB'));
    await tester.tap(find.bySemanticsLabel('Rút đơn CLB'));
    await tester.pumpAndSettle();
    expect(repository.withdrawn, [testOpenClubId]);
    expect(find.bySemanticsLabel('Đăng ký CLB'), findsOneWidget);
  });

  testWidgets('canonicalizes uppercase and rejects malformed deep links', (
    tester,
  ) async {
    final repository = _FakeClubsRepository();
    final router = await _pump(
      tester,
      repository,
      initialLocation: '/clubs/${testOpenClubId.toUpperCase()}',
    );
    addTearDown(router.dispose);
    expect(router.state.uri.path, '/clubs/$testOpenClubId');
    router.go('/clubs/nope');
    await tester.pumpAndSettle();
    expect(router.state.uri.path, AppRoutes.clubs);
  });
}

Future<GoRouter> _pump(
  WidgetTester tester,
  ClubsRepository repository, {
  String initialLocation = AppRoutes.clubs,
}) async {
  final router = createAppRouter(initialLocation: initialLocation);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [clubsRepositoryProvider.overrideWithValue(repository)],
      child: FptSchoolsApp(router: router),
    ),
  );
  await tester.pumpAndSettle();
  return router;
}

final class _FakeClubsRepository implements ClubsRepository {
  final Map<String, SchoolClub> clubs = {
    for (final club in testClubsFeed().clubs) club.id: club,
  };
  final List<ClubCategory?> categories = [];
  final List<String> applied = [];
  final List<String> withdrawn = [];

  @override
  Future<ClubsFeed> getClubs({ClubCategory? category}) {
    categories.add(category);
    return Future.value(
      ClubsFeed(
        clubs: clubs.values
            .where((club) => category == null || club.category == category)
            .toList(),
      ),
    );
  }

  @override
  Future<SchoolClub> getClub(String clubId) => Future.value(clubs[clubId]!);

  @override
  Future<SchoolClub> apply(String clubId) {
    applied.add(clubId);
    clubs[clubId] = SchoolClub.fromJson(
      testClubJson(
        membershipStatus: 'PENDING',
        canApply: false,
        canWithdraw: true,
      ),
    );
    return Future.value(clubs[clubId]!);
  }

  @override
  Future<SchoolClub> withdraw(String clubId) {
    withdrawn.add(clubId);
    clubs[clubId] = SchoolClub.fromJson(
      testClubJson(
        membershipStatus: 'WITHDRAWN',
        canApply: true,
        canWithdraw: false,
      ),
    );
    return Future.value(clubs[clubId]!);
  }
}
