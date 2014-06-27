/*! SET storage_engine=INNODB */;

DROP TABLE IF EXISTS moneris_transactions;
CREATE TABLE moneris_transactions (
  record_id int(11) unsigned NOT NULL AUTO_INCREMENT
, kb_account_id char(36) NOT NULL
, kb_payment_id char(36) NOT NULL
, kb_transaction_id char(36) NOT NULL
, kb_payment_method_id char(36) NOT NULL
, transaction_type varchar(255) NOT NULL
, amount numeric(10,4) NOT NULL
, currency char(3) NOT NULL
, transaction_amount varchar(255) DEFAULT NULL
, transaction_effective_date varchar(255) DEFAULT NULL
, transaction_status varchar(255) DEFAULT NULL
, transaction_gateway_error varchar(255) DEFAULT NULL
, transaction_gateway_error_code varchar(255) DEFAULT NULL
, transaction_first_payment_reference_id varchar(255) DEFAULT NULL
, transaction_second_payment_reference_id varchar(255) DEFAULT NULL
, receipt_is_visa_debit varchar(255) DEFAULT NULL
, receipt_status_message varchar(255) DEFAULT NULL
, receipt_status_code varchar(255) DEFAULT NULL
, receipt_cavv_result_code varchar(255) DEFAULT NULL
, receipt_cvd_result_code varchar(255) DEFAULT NULL
, receipt_avs_result_code varchar(255) DEFAULT NULL
, receipt_recur_success varchar(255) DEFAULT NULL
, receipt_ticket varchar(255) DEFAULT NULL
, receipt_timed_out varchar(255) DEFAULT NULL
, receipt_txn_number varchar(255) DEFAULT NULL
, receipt_card_type varchar(255) DEFAULT NULL
, receipt_trans_amount varchar(255) DEFAULT NULL
, receipt_message varchar(255) DEFAULT NULL
, receipt_complete varchar(255) DEFAULT NULL
, receipt_trans_type varchar(255) DEFAULT NULL
, receipt_trans_date varchar(255) DEFAULT NULL
, receipt_trans_time varchar(255) DEFAULT NULL
, receipt_auth_code varchar(255) DEFAULT NULL
, receipt_iso varchar(255) DEFAULT NULL
, receipt_response_code varchar(255) DEFAULT NULL
, receipt_reference_num varchar(255) DEFAULT NULL
, receipt_receipt_id varchar(255) DEFAULT NULL
, created_by varchar(50) NOT NULL
, created_date datetime NOT NULL
, updated_by varchar(50) DEFAULT NULL
, updated_date datetime DEFAULT NULL
, kb_tenant_id char(36) DEFAULT NULL
, PRIMARY KEY(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
CREATE INDEX moneris_transactions_kb_payment_id_kb_tenant_id ON moneris_transactions(kb_payment_id, kb_tenant_id);

DROP TABLE IF EXISTS moneris_payment_methods;
CREATE TABLE moneris_payment_methods (
  record_id int(11) unsigned NOT NULL AUTO_INCREMENT
, kb_account_id char(36) NOT NULL
, kb_payment_method_id char(36) NOT NULL
, external_payment_method_id varchar(255) NOT NULL
, is_deleted bool DEFAULT FALSE
, created_by varchar(50) NOT NULL
, created_date datetime NOT NULL
, updated_by varchar(50) DEFAULT NULL
, updated_date datetime DEFAULT NULL
, kb_tenant_id char(36) DEFAULT NULL
, PRIMARY KEY(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
CREATE UNIQUE INDEX moneris_payment_methods_kb_payment_method_id ON moneris_payment_methods(kb_payment_method_id);
CREATE INDEX moneris_payment_methods_kb_payment_method_id_kb_tenant_id ON moneris_payment_methods(kb_payment_method_id, kb_tenant_id);
CREATE INDEX moneris_payment_methods_kb_account_id_kb_tenant_id ON moneris_payment_methods(kb_account_id, kb_tenant_id);
