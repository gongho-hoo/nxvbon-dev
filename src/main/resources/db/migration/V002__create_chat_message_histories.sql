create table if not exists chat_message_histories (
    id varchar(64) primary key,
    project_id varchar(64),
    session_id varchar(128) not null,
    role varchar(30) not null,
    content text not null,
    metadata_json text,
    created_at timestamp with time zone not null
);

create index if not exists idx_chat_message_histories_session_created_at
    on chat_message_histories(session_id, created_at);

create index if not exists idx_chat_message_histories_project_session_created_at
    on chat_message_histories(project_id, session_id, created_at);

create index if not exists idx_chat_message_histories_created_at
    on chat_message_histories(created_at);
