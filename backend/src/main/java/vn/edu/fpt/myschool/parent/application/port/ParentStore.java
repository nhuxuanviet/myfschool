package vn.edu.fpt.myschool.parent.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.parent.domain.ParentChild;

/**
 * A guardian's access to their own children.
 *
 * <p>The guardian is resolved from the authenticated user; the child id from the client is only
 * ever checked against the link, never trusted.
 */
public interface ParentStore {

    Optional<UUID> findParentIdByUserId(UUID userId);

    List<ParentChild> findChildren(UUID parentId);

    /** True only while a link between this guardian and this student is in force today. */
    boolean isLinkedToStudent(UUID parentId, UUID studentId);
}
