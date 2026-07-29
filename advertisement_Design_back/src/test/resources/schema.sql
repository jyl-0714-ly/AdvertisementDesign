CREATE TABLE idempotency_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operation_type VARCHAR(64) NOT NULL,
  actor_type VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL,
  resource_type VARCHAR(64), resource_id BIGINT, response_snapshot JSON, failure_code VARCHAR(64),
  expires_at TIMESTAMP, version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_idempotency_actor_operation_key UNIQUE(actor_type, actor_id, operation_type, idempotency_key)
);
CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, project_id BIGINT, actor_type VARCHAR(32) NOT NULL, actor_id BIGINT,
  customer_display_identity VARCHAR(64), source VARCHAR(32) NOT NULL, object_type VARCHAR(64) NOT NULL,
  object_id BIGINT, object_version VARCHAR(64), action VARCHAR(64) NOT NULL, authorization_basis JSON,
  before_state JSON, after_state JSON, result VARCHAR(16) NOT NULL, failure_code VARCHAR(64), request_id VARCHAR(128) NOT NULL,
  correlation_id VARCHAR(128), occurred_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_audit_request_object_action UNIQUE(request_id, object_type, object_id, action)
);
CREATE TABLE outbox_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, aggregate_type VARCHAR(64) NOT NULL, aggregate_id BIGINT NOT NULL,
  event_type VARCHAR(128) NOT NULL, event_key VARCHAR(128) NOT NULL UNIQUE, payload JSON NOT NULL,
  status VARCHAR(16) NOT NULL, available_at TIMESTAMP NOT NULL, published_at TIMESTAMP, retry_count INT NOT NULL,
  last_error_code VARCHAR(64), version BIGINT NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
);
