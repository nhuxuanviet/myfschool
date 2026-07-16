import 'package:myfschoolse1913/src/features/clubs/domain/school_club.dart';

const testOpenClubId = '88888888-8888-4888-8888-888888888881';
const testPendingClubId = '88888888-8888-4888-8888-888888888882';

Map<String, dynamic> testClubJson({
  String id = testOpenClubId,
  String category = 'ACADEMIC',
  String name = 'CLB Khoa học',
  int? capacity = 30,
  int activeMemberCount = 10,
  String membershipStatus = 'NOT_APPLIED',
  bool canApply = true,
  bool canWithdraw = false,
}) => {
  'id': id,
  'category': category,
  'name': name,
  'description': 'Thực hiện các dự án sáng tạo khoa học.',
  'advisorName': 'Cô Nguyễn Thu Hà',
  'meetingSchedule': 'Thứ Tư, 16:00–17:30',
  'location': 'Phòng Lab STEM',
  'audienceGradeLevel': null,
  'capacity': capacity,
  'activeMemberCount': activeMemberCount,
  'applicationDeadline': '2026-08-10T10:00:00Z',
  'membershipStatus': membershipStatus,
  'canApply': canApply,
  'canWithdraw': canWithdraw,
};

Map<String, dynamic> testClubsFeedJson() => {
  'clubs': [
    testClubJson(),
    testClubJson(
      id: testPendingClubId,
      category: 'MEDIA',
      name: 'CLB Truyền thông',
      membershipStatus: 'PENDING',
      canApply: false,
      canWithdraw: true,
    ),
  ],
};

ClubsFeed testClubsFeed() => ClubsFeed.fromJson(testClubsFeedJson());
