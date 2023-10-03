-- drop table if exists transaction_item cascade;
-- drop table if exists transaction cascade;
-- drop table if exists account cascade;
-- drop table if exists outbox cascade;

drop type if exists account_type;

create type if not exists account_type as enum ('A', 'L', 'E', 'R', 'C');

drop sequence if exists account_name_sequence;

create sequence if not exists account_name_sequence
    start 1 increment by 64 cache 64;

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
    region         string         not null default crdb_internal.locality_value('region'),
    metadata       jsonb          null,

    primary key (id)
);

create table if not exists transaction
(
    id               int    not null default unordered_unique_rowid(),
    token            uuid   not null unique,
    booking_date     date   not null default current_date(),
    transfer_date    date   not null default current_date(),
    transaction_type string(3) not null,
    region           string not null default crdb_internal.locality_value('region'),

    primary key (id)
);

create table if not exists transaction_item
(
    transaction_id        int            not null,
    account_id            int            not null,
    amount                decimal(19, 2) not null,
    currency              string(3) not null,
    amount_money          string as (concat(amount::string, ' ', currency)) virtual,
    note                  string,
    running_balance       decimal(19, 2) not null,
    running_balance_money string as (concat(running_balance::string, ' ', currency)) virtual,
    region                string         not null default crdb_internal.locality_value('region'),

    primary key (transaction_id, account_id)
);

------------------------------------------------
-- Constraints on account
------------------------------------------------

alter table if exists account
    add constraint check_account_allow_negative check (allow_negative between 0 and 1);
alter table if exists account
    add constraint check_account_positive_balance check (balance * abs(allow_negative - 1) >= 0);

------------------------------------------------
-- Constraints on transaction_item
------------------------------------------------

alter table transaction_item
    add constraint fk_item_ref_transaction
        foreign key (transaction_id) references transaction (id);

alter table transaction_item
    add constraint fk_item_ref_account
        foreign key (account_id) references account (id);


create table outbox
(
    id             uuid        not null default gen_random_uuid(),
    create_time    timestamptz not null default clock_timestamp(),
    aggregate_type string      not null,
    aggregate_id   string      null,
    event_type     string      not null,
    payload        jsonb       not null,

    primary key (id)
);
