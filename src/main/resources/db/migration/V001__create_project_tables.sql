create table if not exists projects (
    id varchar(64) primary key,
    name varchar(255) not null,
    description text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists scm_repositories (
    id varchar(64) primary key,
    project_id varchar(64) not null references projects(id),
    scm_type varchar(20) not null,
    repository_url text not null,
    default_ref varchar(512),
    working_copy_path text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists audit_logs (
    id varchar(64) primary key,
    actor varchar(255),
    action varchar(255) not null,
    target_type varchar(255),
    target_id varchar(255),
    detail text,
    created_at timestamp with time zone not null
);
