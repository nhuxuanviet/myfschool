package vn.edu.fpt.myschool.shared.time;

import java.time.ZoneId;

/** Time zone used for Vietnamese school-calendar dates and local lesson times. */
public final class SchoolTimeZone {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private SchoolTimeZone() {
    }
}
