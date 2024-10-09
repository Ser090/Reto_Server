/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  Sergio
 * Created: 09-oct-2024
 */

-- Table: public.datos_usuario

-- DROP TABLE IF EXISTS public.datos_usuario;

CREATE TABLE IF NOT EXISTS public.datos_usuario
(
    id integer NOT NULL DEFAULT nextval('res_users_id_seq'::regclass),
    res_userid integer,
    nombre character varying(50) COLLATE pg_catalog."default",
    apellido character varying(50) COLLATE pg_catalog."default",
    fecha_nacimiento date,
    provincia character varying(50) COLLATE pg_catalog."default",
    localidad character varying(50) COLLATE pg_catalog."default",
    telefono character varying(15) COLLATE pg_catalog."default",
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT datos_usuario_pkey PRIMARY KEY (id),
    CONSTRAINT datos_usuario_res_userid_fkey FOREIGN KEY (res_userid)
        REFERENCES public.res_users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.datos_usuario
    OWNER to sergio;

