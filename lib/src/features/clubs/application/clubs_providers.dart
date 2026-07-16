import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/clubs_api_repository.dart';
import '../domain/clubs_repository.dart';
import '../domain/school_club.dart';

final clubsRepositoryProvider = Provider<ClubsRepository>(
  (ref) => ApiClubsRepository(ref.watch(authenticatedApiClientProvider)),
);
final clubCategoryFilterProvider =
    NotifierProvider.autoDispose<_ClubFilter, ClubCategory?>(_ClubFilter.new);

final class _ClubFilter extends Notifier<ClubCategory?> {
  @override
  ClubCategory? build() => null;
  void select(ClubCategory? category) => state = category;
}

final clubsFeedProvider = FutureProvider.autoDispose<ClubsFeed>(
  (ref) => ref
      .watch(clubsRepositoryProvider)
      .getClubs(category: ref.watch(clubCategoryFilterProvider)),
);
final clubDetailsProvider = FutureProvider.autoDispose
    .family<SchoolClub, String>((ref, clubId) async {
      final id = canonicalClubId(clubId);
      if (id == null) {
        throw const FormatException('Invalid club ID.');
      }
      final club = await ref.watch(clubsRepositoryProvider).getClub(id);
      if (club.id != id) {
        throw const FormatException('Club response ID mismatch.');
      }
      return club;
    });

final class ClubMutationState {
  const ClubMutationState({this.isSubmitting = false, this.errorMessage});
  final bool isSubmitting;
  final String? errorMessage;
}

final clubMutationProvider =
    NotifierProvider.autoDispose<ClubMutationController, ClubMutationState>(
      ClubMutationController.new,
    );

class ClubMutationController extends Notifier<ClubMutationState> {
  @override
  ClubMutationState build() => const ClubMutationState();
  Future<bool> apply(String id) =>
      _submit(id, ref.read(clubsRepositoryProvider).apply);
  Future<bool> withdraw(String id) =>
      _submit(id, ref.read(clubsRepositoryProvider).withdraw);

  Future<bool> _submit(
    String id,
    Future<SchoolClub> Function(String) action,
  ) async {
    if (state.isSubmitting) {
      return false;
    }
    final canonical = canonicalClubId(id);
    if (canonical == null) {
      return false;
    }
    state = const ClubMutationState(isSubmitting: true);
    try {
      final club = await action(canonical);
      if (!ref.mounted) {
        return false;
      }
      if (club.id != canonical) {
        throw const FormatException('Club response ID mismatch.');
      }
      state = const ClubMutationState();
      ref.invalidate(clubDetailsProvider(canonical));
      ref.invalidate(clubsFeedProvider);
      return true;
    } on Object {
      if (!ref.mounted) {
        return false;
      }
      state = const ClubMutationState(
        errorMessage: 'Không thể cập nhật đăng ký CLB. Vui lòng thử lại.',
      );
      ref.invalidate(clubDetailsProvider(canonical));
      ref.invalidate(clubsFeedProvider);
      return false;
    }
  }
}
