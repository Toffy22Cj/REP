-- Migration script to add file attachment support to respuestas_pregunta table
-- Created: 2026-01-07

-- Add columns for storing file attachments in student answers
ALTER TABLE respuestas_pregunta 
ADD COLUMN archivo_adjunto VARCHAR(500),
ADD COLUMN nombre_archivo VARCHAR(255);

-- Create index for faster queries on file attachments
CREATE INDEX idx_respuestas_archivo ON respuestas_pregunta(archivo_adjunto);

-- Add comments for documentation
COMMENT ON COLUMN respuestas_pregunta.archivo_adjunto IS 'Ruta del archivo adjunto en el servidor';
COMMENT ON COLUMN respuestas_pregunta.nombre_archivo IS 'Nombre original del archivo adjuntado por el estudiante';
