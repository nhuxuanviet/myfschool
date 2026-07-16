import 'school_club.dart';

abstract interface class ClubsRepository {
  Future<ClubsFeed> getClubs({ClubCategory? category});
  Future<SchoolClub> getClub(String clubId);
  Future<SchoolClub> apply(String clubId);
  Future<SchoolClub> withdraw(String clubId);
}
