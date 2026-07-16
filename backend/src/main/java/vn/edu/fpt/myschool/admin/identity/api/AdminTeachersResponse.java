package vn.edu.fpt.myschool.admin.identity.api;

import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

public record AdminTeachersResponse(
        List<Item> items, int page, int size, long totalElements, int totalPages) {

    public record Item(
            UUID id,
            String teacherCode,
            String fullName,
            String email,
            String phoneNumber,
            boolean enabled,
            boolean hasAccount,
            long version) {

        static Item from(AdminIdentity.Teacher teacher) {
            return new Item(
                    teacher.id(),
                    teacher.teacherCode(),
                    teacher.fullName(),
                    teacher.email(),
                    teacher.phoneNumber(),
                    teacher.enabled(),
                    teacher.hasAccount(),
                    teacher.version());
        }
    }

    static AdminTeachersResponse from(AdminIdentity.TeacherPage page) {
        return new AdminTeachersResponse(
                page.items().stream().map(Item::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
