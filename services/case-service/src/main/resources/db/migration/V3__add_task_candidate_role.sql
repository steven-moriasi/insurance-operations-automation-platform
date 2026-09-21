alter table case_task add column candidate_role varchar(64);

create index idx_case_task_candidate_role
    on case_task(candidate_role, status, due_at);
