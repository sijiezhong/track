-- Allow event.properties to be nullable to support SDKs not sending custom properties
alter table if exists event alter column properties drop not null;


