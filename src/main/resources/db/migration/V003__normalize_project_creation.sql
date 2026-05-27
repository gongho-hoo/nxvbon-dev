alter table projects add column if not exists root_path text;
alter table projects add column if not exists backend_path text;
alter table projects add column if not exists fastapi_path text;
alter table projects add column if not exists web_ui_path text;
alter table projects add column if not exists vcs_type varchar(20);
alter table projects add column if not exists vcs_url text;
alter table projects add column if not exists default_branch varchar(100);
alter table projects add column if not exists enabled boolean;

update projects set root_path = coalesce(root_path, '') where root_path is null;
update projects set vcs_type = coalesce(vcs_type, 'NONE') where vcs_type is null;
update projects set default_branch = coalesce(default_branch, 'main') where default_branch is null;
update projects set enabled = coalesce(enabled, true) where enabled is null;

alter table projects alter column root_path set not null;
alter table projects alter column enabled set not null;
