-- Добавление колонки file_key в таблицу import_history
-- Выполните этот SQL на вашей базе данных PostgreSQL

ALTER TABLE import_history 
ADD COLUMN IF NOT EXISTS file_key VARCHAR(255);

-- Проверка, что колонка добавлена
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'import_history' AND column_name = 'file_key';

