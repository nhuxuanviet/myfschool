-- Approving a form is an act someone is answerable for, and V10 recorded no one:
-- student_form_status_history says a form was approved but not by whom (spec §5.7).
-- A trail that cannot name the actor is not a trail.

-- Who raised the form, and as what. A guardian may file for their child, so the
-- student the form is about is not the same question as who submitted it.
ALTER TABLE student_forms ADD COLUMN submitted_by UUID;
ALTER TABLE student_forms ADD COLUMN submitted_by_role VARCHAR(16);

-- Existing forms predate guardians filing anything, so the student themselves
-- raised every one of them. Deriving it is honest here; inventing an actor for
-- the approvals below would not be.
UPDATE student_forms form
SET submitted_by = student.user_id,
    submitted_by_role = 'STUDENT'
FROM students student
WHERE student.id = form.student_id;

ALTER TABLE student_forms
    ADD CONSTRAINT fk_student_forms_submitted_by FOREIGN KEY (submitted_by)
    REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE student_forms
    ADD CONSTRAINT ck_student_forms_submitted_by_role
    CHECK (submitted_by_role IS NULL OR submitted_by_role IN ('STUDENT', 'PARENT'));

ALTER TABLE student_form_status_history ADD COLUMN actor_user_id UUID;
ALTER TABLE student_form_status_history ADD COLUMN actor_role VARCHAR(16);

ALTER TABLE student_form_status_history
    ADD CONSTRAINT fk_student_form_status_history_actor FOREIGN KEY (actor_user_id)
    REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE student_form_status_history
    ADD CONSTRAINT ck_student_form_status_history_actor_role
    CHECK (actor_role IS NULL OR actor_role IN ('STUDENT', 'PARENT', 'TEACHER', 'ADMIN'));

-- Left NULL for historical rows on purpose. Nobody knows who approved those, and
-- filling in a plausible name would turn an admitted gap into a false record.
-- New transitions are written with an actor by the application.

CREATE INDEX ix_student_forms_submitted_by
    ON student_forms (submitted_by, submitted_at DESC)
    WHERE submitted_by IS NOT NULL;
