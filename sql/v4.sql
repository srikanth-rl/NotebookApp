USE notebook_app;
-- ---------------------------------------------------------
--  Remove duplicate phones for SAME user
--     (keep the newest row)
-- ---------------------------------------------------------
DELETE t1
FROM user_phones t1
JOIN user_phones t2
  ON t1.user_id = t2.user_id
 AND t1.phone_number = t2.phone_number
 AND t1.id < t2.id;



-- ---------------------------------------------------------
-- Ensure ONLY ONE primary per USER
--     (keep newest primary)
-- ---------------------------------------------------------
UPDATE user_phones p1
JOIN (
    SELECT user_id, MAX(created_at) AS max_date
    FROM user_phones
    WHERE is_primary = 1
    GROUP BY user_id
) p2
ON p1.user_id = p2.user_id
SET p1.is_primary = 0
WHERE p1.is_primary = 1
  AND p1.created_at < p2.max_date;



-- ---------------------------------------------------------
--  Ensure ONLY ONE primary per PHONE NUMBER (GLOBAL)
--     (keep newest primary)
-- ---------------------------------------------------------
UPDATE user_phones p1
JOIN (
    SELECT phone_number, MAX(created_at) AS max_date
    FROM user_phones
    WHERE is_primary = 1
    GROUP BY phone_number
) p2
ON p1.phone_number = p2.phone_number
SET p1.is_primary = 0
WHERE p1.is_primary = 1
  AND p1.created_at < p2.max_date;

-- ---------------------------------------------------------
-- Rule 1: Same user cannot add same phone twice
-- ---------------------------------------------------------
ALTER TABLE user_phones
ADD CONSTRAINT unique_user_phone
UNIQUE (user_id, phone_number);



-- ---------------------------------------------------------
-- Rule 2: Only ONE primary per USER
-- ---------------------------------------------------------
CREATE UNIQUE INDEX unique_primary_per_user
ON user_phones (
  (CASE WHEN is_primary = 1 THEN user_id END)
);


-- ---------------------------------------------------------
-- Rule 3: Same phone can be PRIMARY for ONLY ONE user
-- ---------------------------------------------------------
CREATE UNIQUE INDEX unique_primary_phone
ON user_phones (
  (CASE WHEN is_primary = 1 THEN phone_number END)
);