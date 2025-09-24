-- ================================
-- V2__roles_and_tags.sql
-- Adds roles (M2M) and flexible tags (like Reddit: spoiler/NSFW/flair/topic)
-- ================================

-- citext for case-insensitive unique names
CREATE EXTENSION IF NOT EXISTS citext;

-- ROLES (start with roles; you can add permissions later without breaking)
CREATE TABLE IF NOT EXISTS roles (
                                     id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                     name        citext NOT NULL UNIQUE,         -- e.g. 'admin','moderator','user'
                                     description text,
                                     created_at  timestamptz DEFAULT now()
);

-- link users<->roles (many-to-many)
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id uuid NOT NULL,
                                          role_id uuid NOT NULL,
                                          granted_at timestamptz DEFAULT now(),
                                          PRIMARY KEY (user_id, role_id)
);

-- FKs with NOT VALID to minimize locks; validate afterward
ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) NOT VALID;

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) NOT VALID;

ALTER TABLE user_roles VALIDATE CONSTRAINT fk_user_roles_user;
ALTER TABLE user_roles VALIDATE CONSTRAINT fk_user_roles_role;

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);

INSERT INTO roles (name, description) VALUES
                                          ('ADMIN','Full administrative privileges'),
                                          ('MODERATOR','Moderation tools on community content'),
                                          ('USER','Default member')
ON CONFLICT (name) DO NOTHING;

-- TAGS (generic, covers spoiler/nsfw/flair/topics)
-- Use an enum for tag kind; flexible if you want to add more later.
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tag_kind') THEN
            CREATE TYPE tag_kind AS ENUM ('TOPIC','FLAIR','CONTENT_WARNING');
        END IF;
    END$$;

CREATE TABLE IF NOT EXISTS tags (
                                    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                    slug          citext NOT NULL UNIQUE,     -- e.g. 'spoiler','nsfw','java','horror'
                                    display_name  varchar(64) NOT NULL,       -- human label
                                    kind          tag_kind NOT NULL DEFAULT 'TOPIC',
                                    is_spoiler    boolean NOT NULL DEFAULT false, -- convenience flags
                                    is_nsfw       boolean NOT NULL DEFAULT false,  -- so you can filter quickly
                                    color_hex     varchar(7),                      -- optional FLAIR color like '#FF4500'
                                    created_at    timestamptz DEFAULT now(),
                                    updated_at    timestamptz DEFAULT now()
);

-- 4) POST_TAGS (M2M posts<->tags)
CREATE TABLE IF NOT EXISTS post_tags (
                                         post_id uuid NOT NULL,
                                         tag_id  uuid NOT NULL,
                                         added_at timestamptz DEFAULT now(),
                                         added_by_user_id uuid,                       -- who applied the tag (optional)
                                         PRIMARY KEY (post_id, tag_id)
);

-- FKs with NOT VALID then VALIDATE
ALTER TABLE post_tags
    ADD CONSTRAINT fk_post_tags_post
        FOREIGN KEY (post_id) REFERENCES posts(id) NOT VALID;

ALTER TABLE post_tags
    ADD CONSTRAINT fk_post_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(id) NOT VALID;

ALTER TABLE post_tags
    ADD CONSTRAINT fk_post_tags_added_by
        FOREIGN KEY (added_by_user_id) REFERENCES users(id) NOT VALID;

ALTER TABLE post_tags VALIDATE CONSTRAINT fk_post_tags_post;
ALTER TABLE post_tags VALIDATE CONSTRAINT fk_post_tags_tag;
ALTER TABLE post_tags VALIDATE CONSTRAINT fk_post_tags_added_by;

CREATE INDEX IF NOT EXISTS idx_post_tags_post ON post_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag  ON post_tags(tag_id);

-- Seed common content-warning tags (Reddit-like)
INSERT INTO tags (slug, display_name, kind, is_spoiler, is_nsfw)
VALUES
    ('spoiler','Spoiler','CONTENT_WARNING', true, false),
    ('nsfw','NSFW','CONTENT_WARNING', false, true)
ON CONFLICT (slug) DO NOTHING;

-- Example: a generic "announcement" FLAIR (colored)
INSERT INTO tags (slug, display_name, kind, color_hex)
VALUES ('announcement','Announcement','FLAIR','#FF4500')
ON CONFLICT (slug) DO NOTHING;

-- helper indexes for fast filtering of content warnings
CREATE INDEX IF NOT EXISTS idx_tags_content_flags
    ON tags(kind, is_spoiler, is_nsfw);

