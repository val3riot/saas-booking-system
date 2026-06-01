alter table bookings add column cancelled_at timestamp with time zone;
alter table bookings add column cancelled_by_user_id bigint;
alter table bookings add column cancellation_reason varchar(255);
alter table bookings add constraint fk_bookings_cancelled_by_user foreign key (cancelled_by_user_id) references app_users (id);
