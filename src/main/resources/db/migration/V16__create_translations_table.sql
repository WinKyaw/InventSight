-- Create translations table for multi-language support
CREATE TABLE IF NOT EXISTS translations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(200) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    value TEXT NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT translations_key_language_unique UNIQUE (key, language_code)
);

-- Create index for faster language lookups
CREATE INDEX IF NOT EXISTS idx_translations_language_code ON translations(language_code);

-- Create index for category filtering
CREATE INDEX IF NOT EXISTS idx_translations_category ON translations(category);

-- Create index for category and language lookups
CREATE INDEX IF NOT EXISTS idx_translations_category_language ON translations(category, language_code);
