package vn.edu.fpt.myschool.assistant.application;

import java.util.regex.Pattern;

public final class AssistantPlainText {

    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^]]+)]\\([^)]+\\)");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s*#{1,6}\\s*");
    private static final Pattern LIST_ITEM = Pattern.compile("(?m)^\\s*[-+]\\s+");
    private static final Pattern EXCESSIVE_BLANK_LINES = Pattern.compile("\\n{3,}");

    private AssistantPlainText() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String plainText = MARKDOWN_LINK.matcher(value).replaceAll("$1");
        plainText = HEADING.matcher(plainText).replaceAll("");
        plainText = LIST_ITEM.matcher(plainText).replaceAll("• ");
        plainText = plainText.replace("**", "")
                .replace("__", "")
                .replace("*", "")
                .replace("`", "");
        return EXCESSIVE_BLANK_LINES.matcher(plainText).replaceAll("\n\n").strip();
    }

    public static String sanitizeChunk(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("*", "")
                .replace("`", "")
                .replace("#", "");
    }
}
