package vn.edu.fpt.myschool.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import vn.edu.fpt.myschool.assistant.application.AssistantPlainText;

class AssistantPlainTextTest {

    @Test
    void removesCommonMarkdownSyntax() {
        String markdown = """
                ## Lịch học
                - **Thứ Hai:** [Toán](https://example.test)
                - `Ngữ văn`
                """;

        assertThat(AssistantPlainText.sanitize(markdown))
                .isEqualTo("Lịch học\n• Thứ Hai: Toán\n• Ngữ văn");
    }
}
