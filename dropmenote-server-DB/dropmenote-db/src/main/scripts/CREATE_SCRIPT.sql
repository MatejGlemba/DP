
    alter table PUBLIC.BLACKLIST 
        drop constraint if exists FK_897mp5iq4x5ux2yy7t4347ipq;

    alter table PUBLIC.DEVICE 
        drop constraint if exists FK_dqcdo8mqn2arqi85it33pfjfm;

    alter table PUBLIC.QR_CODE 
        drop constraint if exists FK_9ulp8akvjhegpkgutecwc6ndg;

    alter table PUBLIC.QR_CODE_USER_SHARE 
        drop constraint if exists FK_qoyfm8i5x1nexbexhsc827iqs;

    alter table PUBLIC.QR_CODE_USER_SHARE 
        drop constraint if exists FK_pa4t27hly0i8lapcfuj479s6p;

    alter table PUBLIC.QR_CODE_USER_SHARE 
        drop constraint if exists FK_71sth5sgaigm66c8e9moua50i;

    alter table PUBLIC.USER_SESSION 
        drop constraint if exists FK_rp8y9sij97qkfrob601yivmjv;

    drop table PUBLIC.BLACKLIST cascade;

    drop table PUBLIC.CONFIGURATION cascade;

    drop table PUBLIC.DEVICE cascade;

    drop table PUBLIC.MATRIX cascade;

    drop table PUBLIC.QR_CODE cascade;

    drop table PUBLIC.QR_CODE_LIST cascade;

    drop table PUBLIC.QR_CODE_USER_SHARE cascade;

    drop table PUBLIC.USER cascade;

    drop table PUBLIC.USER_SESSION cascade;

    drop sequence blacklist_id_seq;

    drop sequence configuration_id_seq;

    drop sequence device_id_seq;

    drop sequence matrix_seq;

    drop sequence qr_code_id_seq;

    drop sequence qr_code_list_id_seq;

    drop sequence qr_code_user_share_id_seq;

    drop sequence user_id_seq;

    create table PUBLIC.BLACKLIST (
        id int8 not null,
        created timestamp,
        updated timestamp,
        alias varchar(255) not null,
        note varchar(255) not null,
        uuid varchar(255) not null,
        owner_user_id int8 not null,
        primary key (id)
    );

    create table PUBLIC.CONFIGURATION (
        id int8 not null,
        created timestamp,
        updated timestamp,
        key varchar(255) not null,
        note TEXT,
        value TEXT,
        primary key (id)
    );

    create table PUBLIC.DEVICE (
        id int8 not null,
        created timestamp,
        updated timestamp,
        device_id varchar(255) not null,
        user_id int8 not null,
        primary key (id)
    );

    create table PUBLIC.MATRIX (
        id int8 not null,
        active boolean not null,
        alias varchar(255),
        empty boolean not null,
        matrix_password varchar(255),
        matrix_room_id varchar(255) not null,
        matrix_username varchar(255),
        qr_code_uuid varchar(255) not null,
        user_uuid varchar(255) not null,
        primary key (id)
    );

    create table PUBLIC.QR_CODE (
        id int8 not null,
        created timestamp,
        updated timestamp,
        active boolean not null,
        description TEXT not null,
        email_notification boolean not null,
        icon varchar(255) not null,
        name varchar(255) not null,
        owner_alias varchar(255) not null,
        photo varchar(255),
        push_notification boolean not null,
        type varchar(255) not null,
        uuid varchar(255) not null,
        owner_id int8 not null,
        primary key (id)
    );

    create table PUBLIC.QR_CODE_LIST (
        id int8 not null,
        created timestamp,
        updated timestamp,
        uuid varchar(255) not null,
        primary key (id)
    );

    create table PUBLIC.QR_CODE_USER_SHARE (
        id int8 not null,
        owner_user_id int8 not null,
        qr_code_id int8 not null,
        shared_user_id int8 not null,
        primary key (id)
    );

    create table PUBLIC.USER (
        id int8 not null,
        created timestamp,
        updated timestamp,
        alias varchar(255),
        can_change_password boolean,
        chat_icon varchar(255),
        email_notification boolean not null,
        login varchar(255) not null,
        matrix_password varchar(255) not null,
        matrix_username varchar(255) not null,
        password varchar(255) not null,
        photo varchar(255),
        push_notification boolean not null,
        dmn_ai boolean not null,
        recovery_token varchar(255),
        recovery_token_created timestamp,
        uuid varchar(255) not null,
        primary key (id)
    );

    create table PUBLIC.USER_SESSION (
        id int8 not null,
        created timestamp,
        updated timestamp,
        active boolean not null,
        device_id varchar(255) not null,
        token varchar(255) not null,
        user_id int8 not null,
        primary key (id)
    );

    alter table PUBLIC.CONFIGURATION 
        add constraint UK_1lcxwvdo1lv4xxhmyhh3jbokx unique (key);

    alter table PUBLIC.MATRIX 
        add constraint UK_7uya0bcx89nexfph7hhyntnyw unique (matrix_room_id);

    alter table PUBLIC.QR_CODE 
        add constraint UK_ia7ys7v8s042saayanqanclar unique (uuid);

    alter table PUBLIC.QR_CODE_LIST 
        add constraint UK_rkrft4vk6bhetuvp8up9l1ooi unique (uuid);

    alter table PUBLIC.USER 
        add constraint UK_slockai06wyhy7i5c8vnd2o31 unique (login);

    alter table PUBLIC.USER 
        add constraint UK_5lgyif8b7roly5pwdj4g5waj4 unique (uuid);

    alter table PUBLIC.USER_SESSION 
        add constraint UK_5bp2xp722mq3noh5xx5efaf2g unique (token);

    alter table PUBLIC.BLACKLIST 
        add constraint FK_897mp5iq4x5ux2yy7t4347ipq 
        foreign key (owner_user_id) 
        references PUBLIC.USER;

    alter table PUBLIC.DEVICE 
        add constraint FK_dqcdo8mqn2arqi85it33pfjfm 
        foreign key (user_id) 
        references PUBLIC.USER;

    alter table PUBLIC.QR_CODE 
        add constraint FK_9ulp8akvjhegpkgutecwc6ndg 
        foreign key (owner_id) 
        references PUBLIC.USER;

    alter table PUBLIC.QR_CODE_USER_SHARE 
        add constraint FK_qoyfm8i5x1nexbexhsc827iqs 
        foreign key (owner_user_id) 
        references PUBLIC.USER;

    alter table PUBLIC.QR_CODE_USER_SHARE 
        add constraint FK_pa4t27hly0i8lapcfuj479s6p 
        foreign key (qr_code_id) 
        references PUBLIC.QR_CODE;

    alter table PUBLIC.QR_CODE_USER_SHARE 
        add constraint FK_71sth5sgaigm66c8e9moua50i 
        foreign key (shared_user_id) 
        references PUBLIC.USER;

    alter table PUBLIC.USER_SESSION 
        add constraint FK_rp8y9sij97qkfrob601yivmjv 
        foreign key (user_id) 
        references PUBLIC.USER;

    create sequence blacklist_id_seq;

    create sequence configuration_id_seq;

    create sequence device_id_seq;

    create sequence matrix_seq;

    create sequence qr_code_id_seq;

    create sequence qr_code_list_id_seq;

    create sequence qr_code_user_share_id_seq;

    create sequence user_id_seq;
