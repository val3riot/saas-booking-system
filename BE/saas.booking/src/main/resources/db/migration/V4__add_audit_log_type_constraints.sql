alter table audit_logs
    add constraint ck_audit_logs_event_type check (
        event_type in (
            'BOOKING_CREATED',
            'BOOKING_CONFIRMED',
            'BOOKING_REJECTED',
            'BOOKING_CANCELLED'
        )
    );

alter table audit_logs
    add constraint ck_audit_logs_entity_type check (
        entity_type in ('BOOKING')
    );
