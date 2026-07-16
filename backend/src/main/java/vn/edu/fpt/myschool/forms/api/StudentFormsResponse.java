package vn.edu.fpt.myschool.forms.api;

import java.util.List;

import vn.edu.fpt.myschool.forms.domain.StudentFormSummary;

public record StudentFormsResponse(List<StudentFormSummaryResponse> forms) {

    static StudentFormsResponse from(List<StudentFormSummary> forms) {
        return new StudentFormsResponse(
                forms.stream().map(StudentFormSummaryResponse::from).toList());
    }
}
