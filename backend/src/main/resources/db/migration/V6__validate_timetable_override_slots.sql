CREATE FUNCTION validate_timetable_override_slot()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    term_starts_on DATE;
    term_ends_on DATE;
    has_recurring_entry BOOLEAN;
BEGIN
    SELECT starts_on, ends_on
    INTO term_starts_on, term_ends_on
    FROM academic_terms
    WHERE id = NEW.academic_term_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING
            ERRCODE = '23503',
            MESSAGE = 'Timetable override academic term does not exist';
    END IF;

    IF NEW.lesson_date < term_starts_on OR NEW.lesson_date > term_ends_on THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            MESSAGE = 'Timetable override date must fall within its academic term';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM class_timetable_entries entry
        WHERE entry.academic_term_id = NEW.academic_term_id
          AND entry.class_name = NEW.class_name
          AND entry.day_of_week = EXTRACT(ISODOW FROM NEW.lesson_date)::smallint
          AND entry.session = NEW.session
          AND entry.period_number = NEW.period_number
    )
    INTO has_recurring_entry;

    IF NEW.override_type IN ('CANCELLED', 'REPLACED') AND NOT has_recurring_entry THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            MESSAGE = 'CANCELLED and REPLACED overrides require a recurring timetable entry';
    END IF;

    IF NEW.override_type = 'ADDED' AND has_recurring_entry THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            MESSAGE = 'ADDED overrides require an empty recurring timetable slot';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_timetable_override_slot
BEFORE INSERT OR UPDATE OF academic_term_id, class_name, lesson_date, session, period_number,
    override_type
ON timetable_overrides
FOR EACH ROW
EXECUTE FUNCTION validate_timetable_override_slot();
