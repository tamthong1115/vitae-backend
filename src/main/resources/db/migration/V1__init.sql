CREATE TYPE "user_status" AS ENUM (
  'active',
  'deactivated',
  'banned'
);

CREATE TYPE "friendship_status" AS ENUM (
  'pending',
  'accepted',
  'declined'
);

CREATE TYPE "visibility" AS ENUM (
  'public',
  'friends',
  'private',
  'custom'
);

CREATE TYPE "reaction_type" AS ENUM (
  'like',
  'love',
  'haha',
  'wow',
  'sad',
  'angry',
  'care'
);

CREATE TYPE "media_type" AS ENUM (
  'image',
  'video',
  'audio',
  'file'
);

CREATE TYPE "notification_type" AS ENUM (
  'friend_request',
  'friend_accept',
  'post_reaction',
  'comment',
  'comment_reaction',
  'mention',
  'follow',
  'system'
);

CREATE TYPE "friend_request_status" AS ENUM (
  'pending',
  'declined',
  'cancelled',
  'expired'
);

CREATE TABLE IF NOT EXISTS "users" (
                         "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                         "account_name" varchar(50) NOT NULL,
                         "email" varchar(255) NOT NULL,
                         "password_hash" varchar(255),
                         "full_name" varchar(100),
                         "status" user_status NOT NULL DEFAULT 'active',
                         "is_verified" bool DEFAULT false,
                         "two_factor_enabled" bool DEFAULT false,
                         "totp_secret_encrypted" text,
                         "last_login_at" timestamptz,
                         "deleted_at" timestamptz,
                         "created_at" timestamptz DEFAULT (now()),
                         "updated_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "user_profiles" (
                                 "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                 "user_id" uuid NOT NULL,
                                 "bio" text,
                                 "profile_picture_id" uuid,
                                 "cover_photo_id" uuid,
                                 "phone_number" varchar(20),
                                 "address" varchar(255),
                                 "city" varchar(100),
                                 "country" varchar(100),
                                 "website_url" varchar(255),
                                 "education_level" varchar(100),
                                 "date_of_birth" date,
                                 "gender" varchar(20),
                                 "created_at" timestamptz DEFAULT (now()),
                                 "updated_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "oauth_accounts" (
                                  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                  "user_id" uuid NOT NULL,
                                  "provider" varchar(50) NOT NULL,
                                  "provider_user_id" varchar(255) NOT NULL,
                                  "access_token" text,
                                  "refresh_token" text,
                                  "token_expires_at" timestamptz,
                                  "scope" text,
                                  "created_at" timestamptz DEFAULT (now()),
                                  "updated_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "sessions" (
                            "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                            "user_id" uuid NOT NULL,
                            "refresh_token_hash" text NOT NULL,
                            "user_agent" text,
                            "ip" inet,
                            "device_id" varchar(100),
                            "last_used_at" timestamptz,
                            "valid_until" timestamptz NOT NULL,
                            "revoked_at" timestamptz,
                            "revoked_reason" text,
                            "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "friend_requests" (
                                   "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                   "requester_id" uuid NOT NULL,
                                   "addressee_id" uuid NOT NULL,
                                   "status" friend_request_status NOT NULL DEFAULT 'pending',
                                   "message" varchar(200),
                                   "seen_at" timestamptz,
                                   "responded_at" timestamptz,
                                   "expires_at" timestamptz,
                                   "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "friendships" (
                               "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                               "user_id" uuid NOT NULL,
                               "friend_id" uuid NOT NULL,
                               "accepted_at" timestamptz DEFAULT (now()),
                               "created_at" timestamptz DEFAULT (now()),
                               "updated_at" timestamptz DEFAULT (now()),
                               "deleted_at" timestamptz
);

CREATE TABLE IF NOT EXISTS "user_blocks" (
                               "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                               "user_id" uuid NOT NULL,
                               "blocked_user_id" uuid NOT NULL,
                               "reason" text,
                               "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "user_follows" (
                                "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                "follower_id" uuid NOT NULL,
                                "followee_id" uuid NOT NULL,
                                "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "user_families" (
                                 "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                 "user_id" uuid NOT NULL,
                                 "relative_id" uuid NOT NULL,
                                 "relation_type" varchar(50) NOT NULL,
                                 "is_confirmed" bool DEFAULT false,
                                 "created_at" timestamptz DEFAULT (now()),
                                 "updated_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "posts" (
                         "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                         "user_id" uuid NOT NULL,
                         "content" text,
                         "visibility" visibility DEFAULT 'public',
                         "repost_of_post_id" uuid,
                         "is_archived" bool DEFAULT false,
                         "deleted_at" timestamptz,
                         "created_at" timestamptz DEFAULT (now()),
                         "updated_at" timestamptz DEFAULT (now()),
                         "reactions_count" int DEFAULT 0,
                         "comments_count" int DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "media" (
                         "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                         "owner_user_id" uuid NOT NULL,
                         "url" text NOT NULL,
                         "type" media_type NOT NULL DEFAULT 'image',
                         "width" int,
                         "height" int,
                         "duration_seconds" int,
                         "checksum" varchar(128),
                         "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "post_media" (
                              "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                              "post_id" uuid NOT NULL,
                              "media_id" uuid NOT NULL,
                              "sort_order" int DEFAULT 0
);

CREATE TABLE IF NOT EXISTS "comments" (
                            "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                            "post_id" uuid NOT NULL,
                            "user_id" uuid NOT NULL,
                            "parent_comment_id" uuid,
                            "content" text NOT NULL,
                            "thread_path" text,
                            "deleted_at" timestamptz,
                            "created_at" timestamptz DEFAULT (now()),
                            "updated_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "reactions" (
                             "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                             "user_id" uuid NOT NULL,
                             "target_type" varchar(20) NOT NULL,
                             "target_id" uuid NOT NULL,
                             "reaction" reaction_type NOT NULL DEFAULT 'like',
                             "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "notifications" (
                                 "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                 "user_id" uuid NOT NULL,
                                 "actor_user_id" uuid,
                                 "type" notification_type NOT NULL,
                                 "data" jsonb,
                                 "read_at" timestamptz,
                                 "created_at" timestamptz DEFAULT (now())
);

CREATE TABLE IF NOT EXISTS "post_visibility_custom" (
                                          "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
                                          "post_id" uuid NOT NULL,
                                          "allowed_user_id" uuid NOT NULL,
                                          "created_at" timestamptz DEFAULT (now())
);

CREATE UNIQUE INDEX ON "oauth_accounts" ("provider", "provider_user_id");

CREATE INDEX ON "sessions" ("user_id", "valid_until");

CREATE UNIQUE INDEX ON "sessions" ("refresh_token_hash");

CREATE UNIQUE INDEX ON "friend_requests" ("requester_id", "addressee_id");

CREATE INDEX ON "friend_requests" ("addressee_id", "status");

CREATE INDEX ON "friend_requests" ("status", "expires_at");

CREATE UNIQUE INDEX ON "friendships" ("user_id", "friend_id");

CREATE UNIQUE INDEX ON "user_blocks" ("user_id", "blocked_user_id");

CREATE UNIQUE INDEX ON "user_follows" ("follower_id", "followee_id");

CREATE INDEX ON "user_follows" ("followee_id");

CREATE INDEX ON "user_families" ("user_id");

CREATE INDEX ON "user_families" ("relative_id");

CREATE UNIQUE INDEX ON "user_families" ("user_id", "relative_id", "relation_type");

CREATE INDEX ON "posts" ("user_id", "created_at");

CREATE INDEX ON "posts" ("repost_of_post_id");

CREATE INDEX ON "posts" ("visibility");

CREATE INDEX ON "media" ("owner_user_id", "created_at");

CREATE INDEX ON "media" ("checksum");

CREATE INDEX ON "post_media" ("post_id", "sort_order");

CREATE INDEX ON "post_media" ("media_id");

CREATE UNIQUE INDEX ON "post_media" ("post_id", "media_id");

CREATE INDEX ON "comments" ("post_id", "created_at");

CREATE INDEX ON "comments" ("user_id", "created_at");

CREATE INDEX ON "comments" ("parent_comment_id");

CREATE UNIQUE INDEX ON "reactions" ("user_id", "target_type", "target_id");

CREATE INDEX ON "reactions" ("target_type", "target_id");

CREATE INDEX ON "notifications" ("user_id", "created_at");

CREATE INDEX ON "notifications" ("read_at");

CREATE INDEX ON "notifications" ("type");

CREATE UNIQUE INDEX ON "post_visibility_custom" ("post_id", "allowed_user_id");

CREATE INDEX ON "post_visibility_custom" ("allowed_user_id");

COMMENT ON COLUMN "users"."account_name" IS 'Khuyên dùng CITEXT + unique index lower(account_name)';

COMMENT ON COLUMN "users"."email" IS 'Khuyên dùng CITEXT + unique index lower(email)';

COMMENT ON TABLE "friend_requests" IS 'CHECK (requester_id <> addressee_id)';

COMMENT ON TABLE "friendships" IS 'CHECK (user_id <> friend_id)';

COMMENT ON TABLE "user_blocks" IS 'CHECK (user_id <> blocked_user_id) ở SQL';

COMMENT ON TABLE "user_follows" IS 'CHECK (follower_id <> followee_id) ở SQL';

COMMENT ON TABLE "user_families" IS 'CHECK (user_id <> relative_id) ở SQL';

COMMENT ON COLUMN "posts"."reactions_count" IS 'tùy chọn';

COMMENT ON COLUMN "posts"."comments_count" IS 'tùy chọn';

ALTER TABLE "user_profiles" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "user_profiles" ADD FOREIGN KEY ("profile_picture_id") REFERENCES "media" ("id");

ALTER TABLE "user_profiles" ADD FOREIGN KEY ("cover_photo_id") REFERENCES "media" ("id");

ALTER TABLE "oauth_accounts" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "sessions" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "friend_requests" ADD FOREIGN KEY ("requester_id") REFERENCES "users" ("id");

ALTER TABLE "friend_requests" ADD FOREIGN KEY ("addressee_id") REFERENCES "users" ("id");

ALTER TABLE "friendships" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "friendships" ADD FOREIGN KEY ("friend_id") REFERENCES "users" ("id");

ALTER TABLE "user_blocks" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "user_blocks" ADD FOREIGN KEY ("blocked_user_id") REFERENCES "users" ("id");

ALTER TABLE "user_follows" ADD FOREIGN KEY ("follower_id") REFERENCES "users" ("id");

ALTER TABLE "user_follows" ADD FOREIGN KEY ("followee_id") REFERENCES "users" ("id");

ALTER TABLE "user_families" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "user_families" ADD FOREIGN KEY ("relative_id") REFERENCES "users" ("id");

ALTER TABLE "posts" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "posts" ADD FOREIGN KEY ("repost_of_post_id") REFERENCES "posts" ("id");

ALTER TABLE "media" ADD FOREIGN KEY ("owner_user_id") REFERENCES "users" ("id");

ALTER TABLE "post_media" ADD FOREIGN KEY ("post_id") REFERENCES "posts" ("id");

ALTER TABLE "post_media" ADD FOREIGN KEY ("media_id") REFERENCES "media" ("id");

ALTER TABLE "comments" ADD FOREIGN KEY ("post_id") REFERENCES "posts" ("id");

ALTER TABLE "comments" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "comments" ADD FOREIGN KEY ("parent_comment_id") REFERENCES "comments" ("id");

ALTER TABLE "reactions" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "notifications" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "notifications" ADD FOREIGN KEY ("actor_user_id") REFERENCES "users" ("id");

ALTER TABLE "post_visibility_custom" ADD FOREIGN KEY ("post_id") REFERENCES "posts" ("id");

ALTER TABLE "post_visibility_custom" ADD FOREIGN KEY ("allowed_user_id") REFERENCES "users" ("id");