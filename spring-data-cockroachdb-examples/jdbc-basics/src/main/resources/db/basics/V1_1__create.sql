-- drop table if exists account cascade;
-- drop type if exists account_type;

create type if not exists account_type as enum ('A', 'L', 'E', 'R', 'C');

create table if not exists account
(
    id             int            not null default unordered_unique_rowid(),
    balance        decimal(19, 2) not null,
    currency       string(3) not null,
    balance_money  string as (concat(balance::string, ' ', currency)) virtual,
    name           string(128) unique not null,
    description    string(256) null,
    account_type   account_type   not null,
    closed         boolean        not null default false,
    allow_negative integer        not null default 0,
    inserted_at    timestamptz    not null default clock_timestamp(),
    updated_at     timestamptz    null,

    primary key (id)
);

------------------------------------------------
-- Constraints on account
------------------------------------------------

alter table if exists account
    add constraint check_account_allow_negative check (allow_negative between 0 and 1);

alter table if exists account
    add constraint check_account_positive_balance check (balance * abs(allow_negative - 1) >= 0);
