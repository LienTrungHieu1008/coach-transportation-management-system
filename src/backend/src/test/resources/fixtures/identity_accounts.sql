-- Synthetic local/test fixtures only; NOT a Flyway migration or a production seed.
-- Explicitly DISABLED, with a non-credential sentinel instead of a usable password/hash.
-- Insert into a fresh test transaction/database; duplicate IDs/emails deliberately fail.
INSERT INTO identity_account (account_id, email, password_hash, full_name, phone, role, status)
VALUES
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1', 'pa4.passenger.one@example.invalid',
     '!DISABLED_DEMO_NO_CREDENTIAL', 'PA4 Demo Passenger One', '0000000000', 'PASSENGER', 'DISABLED'),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2', 'pa4.passenger.two@example.invalid',
     '!DISABLED_DEMO_NO_CREDENTIAL', 'PA4 Demo Passenger Two', '0000000001', 'PASSENGER', 'DISABLED');
