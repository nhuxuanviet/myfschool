-- V20 required effective_to > effective_from, which made a link created today
-- impossible to end today: the earliest it could stop was tomorrow.
--
-- That is wrong where it matters most. A guardianship can have to end the moment
-- it is revoked — a custody decision, a safeguarding concern — and "access ends
-- tomorrow" is not an answer anyone would accept for a child's data.
--
-- effective_to now means the first day the link no longer applies, so ending it
-- on today's date removes access today. Same-day rows are therefore legitimate
-- and mean exactly "ended, granting nothing".
ALTER TABLE parent_student_links DROP CONSTRAINT ck_parent_student_links_effective_range;
ALTER TABLE parent_student_links
    ADD CONSTRAINT ck_parent_student_links_effective_range
    CHECK (effective_to IS NULL OR effective_to >= effective_from);
