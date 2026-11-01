-- 경매장에 올린 장비는 장착·강화·판매 불가
ALTER TABLE items ADD COLUMN listed BOOLEAN NOT NULL DEFAULT FALSE;

-- 유저 간 장비 거래 (즉시 구매가 방식). 판매 대금에서 수수료 5% 차감
CREATE TABLE auction_listings (
    id              BIGSERIAL   PRIMARY KEY,
    seller_id       BIGINT      NOT NULL REFERENCES heroes (id) ON DELETE CASCADE,
    item_id         BIGINT      NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    price           BIGINT      NOT NULL,
    status          VARCHAR(10) NOT NULL,
    buyer_id        BIGINT      REFERENCES heroes (id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_auction_status ON auction_listings (status);
CREATE INDEX idx_auction_seller ON auction_listings (seller_id);

-- 레이드 주 1회 제한 조회용
CREATE INDEX idx_raid_records_hero_created ON raid_records (hero_id, created_at);
