create table agent_task (
    task_id varchar(64) primary key,
    tenant_id varchar(128) not null,
    subject_id varchar(128) not null,
    actor_id varchar(128) not null,
    idempotency_key varchar(128) not null,
    request_fingerprint varchar(128) not null,
    state varchar(64) not null,
    task_version bigint not null,
    snapshot_json text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_agent_task_scoped_idempotency
        unique (tenant_id, subject_id, actor_id, idempotency_key)
);

create index idx_agent_task_tenant_state_updated
    on agent_task (tenant_id, state, updated_at);

create table agent_audit_event (
    task_id varchar(64) not null references agent_task(task_id),
    sequence_no bigint not null,
    event_type varchar(128) not null,
    actor_id varchar(128) not null,
    detail text not null,
    occurred_at timestamp with time zone not null,
    primary key (task_id, sequence_no)
);

create table agent_confirmation_lock_bucket (
    bucket_id integer primary key
);

insert into agent_confirmation_lock_bucket (bucket_id) values
    (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14),(15),
    (16),(17),(18),(19),(20),(21),(22),(23),(24),(25),(26),(27),(28),(29),(30),(31),
    (32),(33),(34),(35),(36),(37),(38),(39),(40),(41),(42),(43),(44),(45),(46),(47),
    (48),(49),(50),(51),(52),(53),(54),(55),(56),(57),(58),(59),(60),(61),(62),(63);

create table agent_confirmation_decision (
    principal_scope varchar(512) not null,
    idempotency_key varchar(128) not null,
    request_fingerprint varchar(128) not null,
    decision_status varchar(32) not null,
    lease_owner varchar(64),
    lease_until timestamp with time zone,
    task_id varchar(64),
    task_state varchar(64),
    action_id varchar(128),
    tool_status varchar(64),
    audit_id varchar(256),
    task_version bigint,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (principal_scope, idempotency_key)
);
