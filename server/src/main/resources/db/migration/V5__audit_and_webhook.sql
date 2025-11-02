-- Create audit_log table
create table if not exists audit_log (
  id bigserial primary key,
  tenant_id integer,
  username varchar(255),
  method varchar(16),
  path varchar(1024),
  payload text,
  create_time timestamp
);

-- Create webhook_subscription table
create table if not exists webhook_subscription (
  id bigserial primary key,
  tenant_id integer not null,
  url varchar(2048) not null,
  secret varchar(512),
  enabled boolean default true,
  create_time timestamp,
  update_time timestamp
);

create index if not exists idx_audit_tenant_time on audit_log(tenant_id, create_time desc);
create index if not exists idx_webhook_subscription_tenant on webhook_subscription(tenant_id);


