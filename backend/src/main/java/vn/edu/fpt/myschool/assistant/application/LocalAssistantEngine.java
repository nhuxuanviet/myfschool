package vn.edu.fpt.myschool.assistant.application;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import reactor.core.publisher.Flux;

final class LocalAssistantEngine implements AssistantEngine {

    private final StudentAssistantData data;

    LocalAssistantEngine(StudentAssistantData data) {
        this.data = data;
    }

    @Override
    public AssistantReply answer(
            String authenticatedUserId,
            String message,
            List<AssistantConversationMessage> history) {
        String normalized = normalize(message);
        AssistantTopic topic = detectTopic(normalized);
        int dayOffset = detectDayOffset(normalized);
        if (topic == AssistantTopic.UNKNOWN && isFollowUp(normalized)) {
            topic = inferTopic(history);
            if (dayOffset < 0) {
                dayOffset = inferDayOffset(history);
            }
        }
        String answer;
        if (topic == AssistantTopic.PROFILE) {
            answer = data.profile(authenticatedUserId);
        } else if (topic == AssistantTopic.TIMETABLE) {
            if (dayOffset >= 0) {
                answer = data.timetableForDay(authenticatedUserId, dayOffset);
            } else {
                answer = data.timetable(authenticatedUserId);
            }
        } else if (topic == AssistantTopic.GRADES) {
            answer = hasSpecificGradeSubject(normalized)
                    ? formatGradeFact(data.gradeForSubject(authenticatedUserId, message))
                    : data.grades(authenticatedUserId);
        } else if (topic == AssistantTopic.EVENTS) {
            answer = data.events(authenticatedUserId);
        } else if (topic == AssistantTopic.CLUBS) {
            answer = data.clubs(authenticatedUserId);
        } else if (topic == AssistantTopic.FORMS) {
            answer = data.forms(authenticatedUserId);
        } else if (topic == AssistantTopic.ANNOUNCEMENTS) {
            answer = data.announcements(authenticatedUserId);
        } else {
            answer = "Mình có thể giúp em tra cứu hồ sơ học sinh, lịch học, điểm học kỳ, sự kiện, đơn từ, "
                    + "câu lạc bộ và thông báo. Em hãy hỏi cụ thể một nội dung nhé.";
        }
        return new AssistantReply(AssistantPlainText.sanitize(answer), mode());
    }

    @Override
    public Flux<String> stream(
            String authenticatedUserId,
            String message,
            List<AssistantConversationMessage> history) {
        String answer = answer(authenticatedUserId, message, history).answer();
        return Flux.fromIterable(splitIntoChunks(answer));
    }

    @Override
    public AssistantReplyMode mode() {
        return AssistantReplyMode.LOCAL;
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSpecificGradeSubject(String message) {
        String padded = " " + message + " ";
        return containsAny(
                message,
                "toan",
                "ngu van",
                "tieng anh",
                "vat li",
                "hoa hoc",
                "sinh hoc",
                "lich su",
                "dia li",
                "tin hoc",
                "giao duc the chat",
                "the duc")
                || containsAny(
                        padded,
                        " van ",
                        " anh ",
                        " ly ",
                        " li ",
                        " hoa ",
                        " sinh ",
                        " su ",
                        " dia ",
                        " tin ");
    }

    private static String formatGradeFact(SubjectGradeFact fact) {
        if (!fact.found()) {
            return "Không tìm thấy môn " + fact.subjectName() + " trong " + fact.termName() + ".";
        }
        if (!fact.resultAvailable()) {
            return "Môn " + fact.subjectName() + " chưa đủ dữ liệu để có kết quả " + fact.termName() + ".";
        }
        if (fact.termAverage() != null) {
            return "Điểm trung bình " + fact.termName() + " môn " + fact.subjectName() + " là "
                    + fact.termAverage().stripTrailingZeros().toPlainString().replace('.', ',') + ".";
        }
        return "Kết quả " + fact.termName() + " môn " + fact.subjectName() + " là "
                + fact.qualitativeResult() + ".";
    }

    private static AssistantTopic detectTopic(String message) {
        if (containsAny(message, "ho so", "thong tin hoc sinh", "ma hoc sinh", "lop nao", "toi la ai", "em la ai")) {
            return AssistantTopic.PROFILE;
        }
        if (containsAny(message, "lich", "thoi khoa bieu", "tiet hoc", "tiet", "hom nay", "ngay mai", "mai", "nay hoc")) {
            return AssistantTopic.TIMETABLE;
        }
        if (containsAny(message, "diem", "giua ky", "cuoi ky", "mon hoc")) {
            return AssistantTopic.GRADES;
        }
        if (containsAny(message, "su kien", "hoat dong")) {
            return AssistantTopic.EVENTS;
        }
        if (containsAny(message, "cau lac bo", "clb")) {
            return AssistantTopic.CLUBS;
        }
        if (containsAny(message, "don tu", "don xin", "bang diem", "xac nhan")) {
            return AssistantTopic.FORMS;
        }
        if (containsAny(message, "thong bao", "tin moi")) {
            return AssistantTopic.ANNOUNCEMENTS;
        }
        return AssistantTopic.UNKNOWN;
    }

    private static boolean isFollowUp(String message) {
        return containsAny(message, "con", "the", "vay", "do", "noi lai", "them", "co ma");
    }

    private static AssistantTopic inferTopic(List<AssistantConversationMessage> history) {
        for (int index = history.size() - 1; index >= 0; index--) {
            AssistantTopic topic = detectTopic(normalize(history.get(index).content()));
            if (topic != AssistantTopic.UNKNOWN) {
                return topic;
            }
        }
        return AssistantTopic.UNKNOWN;
    }

    private static int detectDayOffset(String message) {
        if (containsAny(message, "ngay mai", "mai")) {
            return 1;
        }
        if (containsAny(message, "hom nay", "nay hoc")) {
            return 0;
        }
        return -1;
    }

    private static int inferDayOffset(List<AssistantConversationMessage> history) {
        for (int index = history.size() - 1; index >= 0; index--) {
            int dayOffset = detectDayOffset(normalize(history.get(index).content()));
            if (dayOffset >= 0) {
                return dayOffset;
            }
        }
        return -1;
    }

    private enum AssistantTopic {
        PROFILE,
        TIMETABLE,
        GRADES,
        EVENTS,
        CLUBS,
        FORMS,
        ANNOUNCEMENTS,
        UNKNOWN
    }

    private static List<String> splitIntoChunks(String value) {
        final int preferredChunkSize = 24;
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int end = Math.min(start + preferredChunkSize, value.length());
            if (end < value.length()) {
                int whitespace = value.lastIndexOf(' ', end);
                if (whitespace > start) {
                    end = whitespace + 1;
                }
            }
            chunks.add(value.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
