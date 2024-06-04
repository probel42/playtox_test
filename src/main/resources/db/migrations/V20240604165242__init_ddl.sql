CREATE TABLE account (
                         id uuid NOT NULL,
                         money bigint NOT NULL,
                         CONSTRAINT account_pk PRIMARY KEY (id)
);
COMMENT ON TABLE account IS E'Счёт';
COMMENT ON COLUMN account.id IS E'Идентификатор';
COMMENT ON COLUMN account.money IS E'Сумма средств';
ALTER TABLE account OWNER TO playtox_app;
