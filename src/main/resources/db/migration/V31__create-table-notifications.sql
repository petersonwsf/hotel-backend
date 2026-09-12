CREATE TABLE IF NOT EXISTS notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_notification_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id)
);

CREATE INDEX idx_notification_user_id ON notification (user_id);