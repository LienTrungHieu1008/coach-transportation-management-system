-- I1 Account prerequisite for Booking foreign keys; not the P01/P02 authentication implementation.
-- Identity owns this table. No account, password or demo data is seeded here.
-- Do not edit this migration after it has been shared or applied; add a new version instead.
CREATE TABLE identity_account (
    account_id UUID NOT NULL,
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    role TEXT NOT NULL,
    status TEXT NOT NULL,
    CONSTRAINT identity_account_pk PRIMARY KEY (account_id),
    CONSTRAINT identity_account_email_valid CHECK (
        char_length(btrim(email)) > 0 AND email = btrim(email)
    ),
    CONSTRAINT identity_account_password_hash_nonblank CHECK (char_length(btrim(password_hash)) > 0),
    CONSTRAINT identity_account_full_name_nonblank CHECK (char_length(btrim(full_name)) > 0),
    CONSTRAINT identity_account_phone_nonblank CHECK (char_length(btrim(phone)) > 0),
    CONSTRAINT identity_account_role_valid CHECK (role IN ('PASSENGER', 'STAFF', 'ADMINISTRATOR')),
    CONSTRAINT identity_account_status_valid CHECK (status IN ('ACTIVE', 'DISABLED'))
);

-- Email is the unique login identifier. Preserve its spelling while preventing casing duplicates.
CREATE UNIQUE INDEX uq_identity_account_email_ci ON identity_account (lower(email));
