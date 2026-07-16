package vn.edu.fpt.myschool.admin.identity.api;

import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

public record AdminParentsResponse(
        List<Item> items, int page, int size, long totalElements, int totalPages) {

    public record Item(
            UUID id,
            String fullName,
            String email,
            String phoneNumber,
            boolean enabled,
            boolean hasAccount,
            int linkedStudents,
            long version) {

        static Item from(AdminIdentity.Parent parent) {
            return new Item(
                    parent.id(),
                    parent.fullName(),
                    parent.email(),
                    parent.phoneNumber(),
                    parent.enabled(),
                    parent.hasAccount(),
                    parent.linkedStudents(),
                    parent.version());
        }
    }

    static AdminParentsResponse from(AdminIdentity.ParentPage page) {
        return new AdminParentsResponse(
                page.items().stream().map(Item::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
