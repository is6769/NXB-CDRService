--liquibase formatted sql
--changeset is6769:0002-create-table-cdrs
create table if not exists cdrs(
    id                  bigint         AUTO_INCREMENT PRIMARY KEY,
    call_type           varchar(200)   not null,
    serviced_msisdn     varchar(200)   not null,
    other_msisdn        varchar(200)   not null,
    start_date_time     datetime       not null,
    finish_date_time    datetime       not null,
    consumed_status     varchar(200)   not null
)
