CREATE TABLE tb_review (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_id BIGINT,
    reservation_id BIGINT NOT NULL UNIQUE,
    room_id BIGINT NOT NULL,
    rating DOUBLE PRECISION NOT NULL,
    comment TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reply TEXT,
    replied_at TIMESTAMP WITHOUT TIME ZONE,
    sentiment VARCHAR(50) NOT NULL,

    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_review_client FOREIGN KEY (client_id) REFERENCES client (id),
    CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_review_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE INDEX idx_review_room_id ON tb_review (room_id);