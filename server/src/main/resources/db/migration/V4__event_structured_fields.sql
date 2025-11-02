-- 为 event 表增加结构化字段（若不存在则添加）
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='ua') THEN
    ALTER TABLE event ADD COLUMN ua VARCHAR(512);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='referrer') THEN
    ALTER TABLE event ADD COLUMN referrer VARCHAR(1024);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='ip') THEN
    ALTER TABLE event ADD COLUMN ip VARCHAR(64);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='device') THEN
    ALTER TABLE event ADD COLUMN device VARCHAR(64);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='os') THEN
    ALTER TABLE event ADD COLUMN os VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='browser') THEN
    ALTER TABLE event ADD COLUMN browser VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='channel') THEN
    ALTER TABLE event ADD COLUMN channel VARCHAR(64);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='event' AND column_name='anonymous_id') THEN
    ALTER TABLE event ADD COLUMN anonymous_id VARCHAR(128);
  END IF;
END $$;


