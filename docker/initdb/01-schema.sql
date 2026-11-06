-- =====================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS
-- SISTEMA PEDAGÓGICO GUALI
-- =====================================================

CREATE DATABASE IF NOT EXISTS guali
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE guali;


-- =====================================================
-- 1. TABLA: leccion
-- =====================================================

CREATE TABLE leccion (
                         id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                         titulo            VARCHAR(350) NOT NULL UNIQUE,
                         contenido_leccion LONGTEXT NOT NULL,
                         objetivo          ENUM(
        'COMPILAR_CON_ERROR',
        'COMPILAR_EXITOSO',
        'EJECUTAR_CON_ERROR',
        'EJECUTAR_EXITOSO'
    ) NOT NULL,
                         fecha_importacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- =====================================================
-- 2. TABLA: leccion_recurso
-- =====================================================

CREATE TABLE leccion_recurso (
                                 id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 leccion_id     BIGINT NOT NULL,
                                 nombre_archivo VARCHAR(250) NOT NULL,
                                 tipo_archivo   VARCHAR(60) NOT NULL,
                                 contenido      LONGBLOB NOT NULL,

                                 UNIQUE KEY uk_recurso (leccion_id, nombre_archivo),

                                 CONSTRAINT fk_recurso_leccion
                                     FOREIGN KEY (leccion_id)
                                         REFERENCES leccion(id)
                                         ON DELETE CASCADE
) ENGINE=InnoDB;


-- =====================================================
-- 3. TABLA: usuario
-- Unifica administradores y estudiantes
-- =====================================================

CREATE TABLE usuario (
                         id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                         tipo           ENUM('ADMIN', 'ESTUDIANTE') NOT NULL,
                         nombre         VARCHAR(250) NOT NULL,
                         codigo         CHAR(8) NULL UNIQUE,
                         acceso_usuario VARCHAR(50) NULL UNIQUE,
                         password_hash  VARCHAR(255) NULL,
                         activo         BOOLEAN NOT NULL DEFAULT TRUE,
                         tamano_fuente  TINYINT NOT NULL DEFAULT 16,
                         tema           ENUM('CLARO', 'OSCURO') NOT NULL DEFAULT 'CLARO',
                         fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT chk_usuario_tipo CHECK (
                             (
                                 tipo = 'ESTUDIANTE'
                                     AND codigo IS NOT NULL
                                     AND acceso_usuario IS NULL
                                     AND password_hash IS NULL
                                 )
                                 OR
                             (
                                 tipo = 'ADMIN'
                                     AND codigo IS NULL
                                     AND acceso_usuario IS NOT NULL
                                     AND password_hash IS NOT NULL
                                 )
                             )
) ENGINE=InnoDB;


-- =====================================================
-- 4. TABLA: usuario_permiso
-- =====================================================

CREATE TABLE usuario_permiso (
                                 usuario_id BIGINT NOT NULL,
                                 permiso ENUM(
        'GESTIONAR_LECCIONES',
        'GESTIONAR_USUARIOS',
        'VER_REPORTES'
    ) NOT NULL,
                                 activo BOOLEAN NOT NULL DEFAULT TRUE,

                                 PRIMARY KEY (usuario_id, permiso),

                                 CONSTRAINT fk_permiso_usuario
                                     FOREIGN KEY (usuario_id)
                                         REFERENCES usuario(id)
                                         ON DELETE CASCADE
) ENGINE=InnoDB;


-- =====================================================
-- 5. TABLA: ingreso
-- =====================================================

CREATE TABLE ingreso (
                         id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                         usuario_id    BIGINT NOT NULL,
                         fecha_ingreso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         ip            VARCHAR(45) NULL,
                         user_agent    VARCHAR(255) NULL,

                         INDEX idx_ingreso_fecha (fecha_ingreso),

                         CONSTRAINT fk_ingreso_usuario
                             FOREIGN KEY (usuario_id)
                                 REFERENCES usuario(id)
                                 ON DELETE CASCADE
) ENGINE=InnoDB;


-- =====================================================
-- 6. TABLA: sesion_leccion
-- =====================================================

CREATE TABLE sesion_leccion (
                                id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                usuario_id     BIGINT NOT NULL,
                                leccion_id     BIGINT NULL,
                                leccion_titulo VARCHAR(350) NOT NULL,
                                fecha_inicio   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                fecha_fin      DATETIME NULL,
                                estado         ENUM('COMPLETADA', 'PENDIENTE')
                   NOT NULL DEFAULT 'PENDIENTE',

                                CONSTRAINT fk_sesion_usuario
                                    FOREIGN KEY (usuario_id)
                                        REFERENCES usuario(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_sesion_leccion
                                    FOREIGN KEY (leccion_id)
                                        REFERENCES leccion(id)
                                        ON DELETE SET NULL
) ENGINE=InnoDB;


-- =====================================================
-- 7. TABLA: intento
-- =====================================================

CREATE TABLE intento (
                         id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                         sesion_leccion_id BIGINT NOT NULL,
                         tipo              ENUM('COMPILAR', 'EJECUTAR') NOT NULL,
                         codigo_fuente     MEDIUMTEXT NOT NULL,
                         exito             BOOLEAN NOT NULL,
                         salida            MEDIUMTEXT NULL,
                         fecha_hora        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_intento_sesion
                             FOREIGN KEY (sesion_leccion_id)
                                 REFERENCES sesion_leccion(id)
                                 ON DELETE CASCADE
) ENGINE=InnoDB;


-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================