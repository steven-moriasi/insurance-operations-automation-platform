alter table settlement add column created_at timestamp with time zone;
update settlement set created_at = instructed_at;
alter table settlement alter column created_at set not null;
alter table settlement alter column instructed_at drop not null;
