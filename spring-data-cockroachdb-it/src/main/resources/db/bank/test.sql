truncate table transaction_item cascade;
truncate table transaction cascade;
truncate table account cascade;

INSERT INTO account (id,region, balance, currency, name, account_type, closed, allow_negative, metadata)
VALUES
    ('000a2746-4710-4c5b-8b1f-368e2a7535c5','us','25000.00','EUR','user:1','A',false,0,'{"quote": "Cockroaches can eat anything", "status": "open"}'),
    ('0031d47b-9b08-4156-9fc6-b9ec1846e48f','us','25000.00','EUR','user:2','A',false,0,'{"quote": "Cockroaches can eat anything", "status": "open"}');

-- ######################################

begin; -- T1
begin; -- T2

SELECT * FROM account WHERE id in ('000a2746-4710-4c5b-8b1f-368e2a7535c5', '0031d47b-9b08-4156-9fc6-b9ec1846e48f') FOR UPDATE; -- T1
SELECT * FROM account WHERE id in ('000a2746-4710-4c5b-8b1f-368e2a7535c5', '0031d47b-9b08-4156-9fc6-b9ec1846e48f') FOR UPDATE; -- T2 (blocks)

UPDATE account
SET balance   = 50276.68,
    updated_at=clock_timestamp()
WHERE id = '000a2746-4710-4c5b-8b1f-368e2a7535c5'
  AND closed = false
  AND currency = 'EUR'
  AND (50276.68) * abs(allow_negative - 1) >= 0; -- T1

INSERT INTO transaction (id, region, booking_date, transfer_date, transaction_type, metadata)
VALUES ('000a2746-4710-4c5b-8b1f-368e2a753598', 'eu', '2022-12-21', '2022-12-21', 'gen',
        '{"quote": "Cockroaches can eat anything", "status": "open"}'); -- T1

INSERT INTO transaction_item (region, transaction_id, account_id, amount, currency, note, running_balance)
VALUES ('eu', '000a2746-4710-4c5b-8b1f-368e2a753598', '000a2746-4710-4c5b-8b1f-368e2a7535c5', '-4.64', 'EUR',
        'Debit note', '50281.32'),
       ('eu', '000a2746-4710-4c5b-8b1f-368e2a753598', '0031d47b-9b08-4156-9fc6-b9ec1846e48f', '4.64', 'EUR',
        'Credit note', '49366.85'); -- T1


commit; -- T1 (unblock t2)

UPDATE account
SET balance   = 50276.68,
    updated_at=clock_timestamp()
WHERE id = '000a2746-4710-4c5b-8b1f-368e2a7535c5'
  AND closed = false
  AND currency = 'EUR'
  AND (50276.68) * abs(allow_negative - 1) >= 0; -- T2

INSERT INTO transaction (id, region, booking_date, transfer_date, transaction_type, metadata)
VALUES ('000a2746-4710-4c5b-8b1f-368e2a753599', 'eu', '2022-12-21', '2022-12-21', 'gen',
        '{"quote": "Cockroaches can eat anything", "status": "open"}'); --T2

INSERT INTO transaction_item (region, transaction_id, account_id, amount, currency, note, running_balance)
VALUES ('eu', '000a2746-4710-4c5b-8b1f-368e2a753599', '000a2746-4710-4c5b-8b1f-368e2a7535c5', '-4.64', 'EUR',
        'Debit note', '50281.32'),
       ('eu', '000a2746-4710-4c5b-8b1f-368e2a753599', '0031d47b-9b08-4156-9fc6-b9ec1846e48f', '4.64', 'EUR',
        'Credit note', '49366.85'); --T2

commit; --T2 (success in CLI)
-- Fail in JDBC:
-- ERROR: restart transaction: TransactionRetryWithProtoRefreshError: WriteTooOldError: write for key /Table/346/1/"\x00\x17\xc8\xcb\x01HJ\\\x83\x123M&\xa0\"a"/0 at timestamp 1671635051.945976708,1 too old; wrote at 1671635051.975530252,1: "sql txn" meta={id=33f0eac5 key=/Table/347/1/"=\x91\x93\xd0L\xc6I`\x9e\xe0o\x19\xb3f}o"/0 pri=0.02240152 epo=0 ts=1671635051.975530252,1 min=1671635051.611433878,0 seq=0} lock=true stat=PENDING rts=1671635051.945976708,1 wto=false gul=1671635052.111433878,0
