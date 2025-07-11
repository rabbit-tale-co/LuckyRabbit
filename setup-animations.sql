-- ==========================================
-- LuckyRabbit Animation Database Setup
-- ==========================================

-- Create animations table
CREATE TABLE IF NOT EXISTS animations (
    id TEXT PRIMARY KEY,
    validation_key TEXT NOT NULL,
    min_api_version TEXT NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create plugin info table
CREATE TABLE IF NOT EXISTS plugininfo (
    latest_version TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Insert default animations for LuckyRabbit v1.1.0
-- These keys are generated using the plugin's encryption system

-- Horizontal Animation
INSERT INTO animations (id, validation_key, min_api_version)
VALUES (
    'HORIZONTAL',
    'LR_ENC:YourEncryptedKeyHere1',
    '1.0.0'
) ON CONFLICT (id) DO UPDATE SET
    validation_key = EXCLUDED.validation_key,
    min_api_version = EXCLUDED.min_api_version,
    updated_at = NOW();

-- Circle Animation
INSERT INTO animations (id, validation_key, min_api_version)
VALUES (
    'CIRCLE',
    'LR_ENC:YourEncryptedKeyHere2',
    '1.0.0'
) ON CONFLICT (id) DO UPDATE SET
    validation_key = EXCLUDED.validation_key,
    min_api_version = EXCLUDED.min_api_version,
    updated_at = NOW();

-- Three in Row Animation
INSERT INTO animations (id, validation_key, min_api_version)
VALUES (
    'THREE_IN_ROW',
    'LR_ENC:YourEncryptedKeyHere3',
    '1.0.0'
) ON CONFLICT (id) DO UPDATE SET
    validation_key = EXCLUDED.validation_key,
    min_api_version = EXCLUDED.min_api_version,
    updated_at = NOW();

-- Set current plugin version
INSERT INTO plugininfo (latest_version)
VALUES ('1.1.0')
ON CONFLICT (latest_version) DO UPDATE SET
    updated_at = NOW();

-- ==========================================
-- Template for adding new animations:
-- ==========================================

/*
-- Custom Animation Template
INSERT INTO animations (id, validation_key, min_api_version)
VALUES (
    'YOUR_ANIMATION_ID',
    'LR_ENC:YourGeneratedKey',
    '1.0.0'
) ON CONFLICT (id) DO UPDATE SET
    validation_key = EXCLUDED.validation_key,
    min_api_version = EXCLUDED.min_api_version,
    updated_at = NOW();
*/

-- ==========================================
-- Verification Queries
-- ==========================================

-- Check all animations
SELECT id,
       LEFT(validation_key, 30) || '...' as validation_key_preview,
       min_api_version,
       created_at,
       updated_at
FROM animations
ORDER BY id;

-- Check plugin version
SELECT * FROM plugininfo;
