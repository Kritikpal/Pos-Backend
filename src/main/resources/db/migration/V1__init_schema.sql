create sequence category_seq start with 1 increment by 50;
create sequence invoice_seq start with 1 increment by 50;
create sequence item_price_seq start with 1 increment by 50;
create sequence menu_item_ingredient_seq start with 1 increment by 50;
create sequence menu_item_seq start with 1 increment by 50;
create sequence menu_recipe_seq start with 1 increment by 50;
create sequence order_tax_seq start with 1 increment by 50;
create sequence orders_seq start with 1 increment by 50;
create sequence product_file_seq start with 1 increment by 50;
create sequence refresh_token_seq start with 1 increment by 50;
create sequence restaurant_chain_seq start with 1 increment by 50;
create sequence restaurant_seq start with 1 increment by 50;
create sequence restaurant_table_seq start with 1 increment by 50;
create sequence sale_item_seq start with 1 increment by 50;
create sequence stock_receipt_item_seq start with 1 increment by 50;
create sequence stock_receipt_seq start with 1 increment by 50;
create sequence supplier_seq start with 1 increment by 50;
create sequence tax_rate_seq start with 1 increment by 50;
create sequence tbl_password_reset_request_seq start with 1 increment by 50;
create sequence tbl_user_seq start with 1 increment by 50;
create sequence user_roles_seq start with 1 increment by 50;

create table category (
    is_active boolean not null,
    is_deleted boolean not null,
    category_id bigint not null,
    created_at timestamp(6),
    restaurant_id bigint,
    updated_at timestamp(6),
    category_description text not null,
    category_name varchar(255) not null,
    primary key (category_id)
);

create table ingredient_stock (
    is_active boolean not null,
    is_deleted boolean not null,
    reorder_level float(53) not null,
    total_stock float(53) not null,
    created_at timestamp(6),
    last_restocked_at timestamp(6),
    restaurant_id bigint not null,
    supplier_id bigint,
    updated_at timestamp(6),
    unit_of_measure varchar(30) not null,
    description text,
    ingredient_name varchar(255) not null,
    sku varchar(255) not null,
    primary key (sku)
);

create table invoice (
    total_amount float(53) not null,
    generated_at timestamp(6) not null,
    id bigint not null,
    order_id bigint not null unique,
    file_path varchar(255) not null,
    invoice_number varchar(255) not null unique,
    primary key (id)
);

create table item_price (
    dis_count float(53) not null,
    price float(53) not null,
    price_id bigint not null,
    primary key (price_id)
);

create table product_file (
    image_id bigint not null,
    upload_time timestamp(6) not null,
    file_name varchar(255) not null,
    file_type varchar(255) not null,
    url varchar(255) not null,
    primary key (image_id)
);

create table restaurant_chain (
    is_active boolean not null,
    is_deleted boolean not null,
    chain_id bigint not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    description varchar(255),
    email varchar(255),
    gst_number varchar(255),
    logo_url varchar(255),
    name varchar(255) not null unique,
    phone_number varchar(255),
    primary key (chain_id)
);

create table supplier (
    is_active boolean not null,
    is_deleted boolean not null,
    created_at timestamp(6) not null,
    restaurant_id bigint not null,
    supplier_id bigint not null,
    updated_at timestamp(6) not null,
    phone_number varchar(20),
    tax_identifier varchar(50),
    email varchar(120),
    address text,
    contact_person varchar(255),
    notes text,
    supplier_name varchar(255) not null,
    primary key (supplier_id)
);

create table user_roles (
    role_id bigint not null,
    role_name varchar(255) unique,
    primary key (role_id)
);

create table restaurant (
    is_active boolean not null,
    is_deleted boolean not null,
    chain_id bigint,
    created_at timestamp(6),
    restaurant_id bigint not null,
    updated_at timestamp(6),
    code varchar(20) not null unique,
    address_line1 varchar(255),
    address_line2 varchar(255),
    city varchar(255),
    country varchar(255),
    currency varchar(255),
    email varchar(255),
    gst_number varchar(255),
    name varchar(255) not null,
    phone_number varchar(255),
    pincode varchar(255),
    state varchar(255),
    timezone varchar(255),
    primary key (restaurant_id)
);

create table item_stock (
    is_active boolean not null,
    is_deleted boolean not null,
    reorder_level integer not null,
    total_stock integer not null,
    created_at timestamp(6),
    last_restocked_at timestamp(6),
    menu_item_id bigint not null unique,
    restaurant_id bigint,
    supplier_id bigint,
    updated_at timestamp(6),
    unit_of_measure varchar(30) not null,
    sku varchar(255) not null,
    primary key (sku)
);

create table menu_item (
    has_recipi boolean,
    is_active boolean not null,
    is_available boolean not null,
    is_deleted boolean not null,
    is_trending boolean not null,
    category_id bigint not null,
    created_at timestamp(6) not null,
    id bigint not null,
    price_id bigint not null unique,
    product_image_image_id bigint unique,
    restaurant_id bigint,
    updated_at timestamp(6),
    description text,
    item_name varchar(255) not null,
    primary key (id)
);

create table menu_recipe (
    active boolean not null,
    batch_size integer not null,
    id bigint not null,
    menu_item_id bigint not null unique,
    primary key (id)
);

create table menu_item_ingredient (
    quantity_required float(53) not null,
    created_at timestamp(6) not null,
    id bigint not null,
    menu_item_id bigint not null,
    recipe_id bigint,
    updated_at timestamp(6),
    ingredient_sku varchar(255) not null,
    primary key (id),
    constraint uk_menu_item_ingredient unique (menu_item_id, ingredient_sku)
);

create table orders (
    is_active boolean not null,
    is_deleted boolean not null,
    payment_status smallint not null check (payment_status between 0 and 4),
    payment_type smallint not null check (payment_type between 0 and 2),
    total_price float(53) not null,
    cancelled_at timestamp(6),
    created_at timestamp(6) not null,
    id bigint not null,
    last_updated_time timestamp(6) not null,
    operator_user_id bigint,
    payment_completed_at timestamp(6),
    payment_initiated_time timestamp(6) not null,
    refund_operator_user_id bigint,
    refunded_at timestamp(6),
    restaurant_id bigint,
    updated_at timestamp(6) not null,
    refund_reason varchar(500),
    payment_notes varchar(1000),
    refund_notes varchar(1000),
    external_txn_id varchar(255),
    order_id varchar(255) not null unique,
    payment_collected_by varchar(255),
    payment_reference varchar(255),
    primary key (id)
);

create table order_tax (
    tax_amount float(53),
    id bigint not null,
    order_id bigint not null,
    tax_name varchar(255),
    primary key (id)
);

create table refresh_token (
    latitude float(53),
    longitude float(53),
    revoked boolean not null,
    created_at timestamp(6),
    expiry_date timestamp(6),
    id bigint not null,
    user_id bigint not null,
    token_id varchar(255) not null unique,
    primary key (id)
);

create table restaurant_table (
    is_active boolean not null,
    is_deleted boolean not null,
    seats integer not null,
    table_number integer not null unique,
    created_at timestamp(6),
    restaurant_id bigint,
    table_id bigint not null,
    updated_at timestamp(6),
    primary key (table_id)
);

create table sale_item (
    amount integer not null,
    is_active boolean not null,
    is_deleted boolean not null,
    sale_item_price float(53) not null,
    created_at timestamp(6),
    manu_item_id bigint,
    order_id bigint not null,
    restaurant_id bigint,
    sale_item_id bigint not null,
    updated_at timestamp(6),
    sale_item_name varchar(255) not null,
    primary key (sale_item_id)
);

create table stock_receipt (
    is_deleted boolean not null,
    total_cost float(53) not null,
    total_items integer not null,
    total_quantity integer not null,
    created_at timestamp(6) not null,
    receipt_id bigint not null,
    received_at timestamp(6) not null,
    restaurant_id bigint not null,
    supplier_id bigint not null,
    updated_at timestamp(6) not null,
    invoice_number varchar(255),
    notes text,
    receipt_number varchar(255) not null unique,
    primary key (receipt_id)
);

create table stock_receipt_item (
    quantity_received integer not null,
    total_cost float(53) not null,
    unit_cost float(53) not null,
    receipt_id bigint not null,
    receipt_item_id bigint not null,
    sku_type varchar(30) not null check (sku_type in ('INGREDIENT','DIRECT_MENU')),
    ingredient_sku varchar(255),
    item_stock_sku varchar(255),
    sku_name varchar(255) not null,
    primary key (receipt_item_id)
);

create table tax_rate (
    is_active boolean not null,
    is_deleted boolean not null,
    tax_amount float(53) not null,
    created_at timestamp(6),
    restaurant_id bigint,
    tax_id bigint not null,
    updated_at timestamp(6),
    tax_name varchar(255) not null,
    primary key (tax_id)
);

create table tbl_password_reset_request (
    created_at timestamp(6) not null,
    expiry_date timestamp(6) not null,
    id bigint not null,
    used_at timestamp(6),
    user_id bigint not null,
    verified_at timestamp(6),
    email varchar(255) not null,
    status varchar(255) not null check (status in ('PENDING','VERIFIED','EXPIRED','USED')),
    token_id varchar(255) not null unique,
    primary key (id)
);

create table tbl_user (
    chain_id bigint,
    restaurant_id bigint,
    user_id bigint not null,
    phone_number varchar(10) not null unique,
    email varchar(255) not null unique,
    password varchar(255) not null,
    primary key (user_id)
);

create table tbl_user_roles (
    roles_role_id bigint not null,
    user_user_id bigint not null,
    primary key (roles_role_id, user_user_id)
);

create index idx_category_name on category (category_name);
create index idx_category_restaurant on category (restaurant_id);
create index idx_ingredient_stock_restaurant on ingredient_stock (restaurant_id);
create index idx_ingredient_stock_name on ingredient_stock (ingredient_name);
create index idx_item_stock_restaurant on item_stock (restaurant_id);
create index idx_menu_item_name on menu_item (item_name);
create index idx_menu_item_restaurant on menu_item (restaurant_id);
create index idx_menu_item_ingredient_menu on menu_item_ingredient (menu_item_id);
create index idx_menu_item_ingredient_sku on menu_item_ingredient (ingredient_sku);
create index idx_order_restaurant_last_updated on orders (restaurant_id, last_updated_time);
create index idx_restaurant_chain on restaurant (chain_id);
create index idx_restaurant_code on restaurant (code);
create index idx_restaurant_city on restaurant (city);
create index idx_chain_name on restaurant_chain (name);
create index idx_restaurant_table_restaurant on restaurant_table (restaurant_id);
create index idx_sale_item_restaurant on sale_item (restaurant_id);
create index idx_stock_receipt_restaurant on stock_receipt (restaurant_id);
create index idx_stock_receipt_item_receipt on stock_receipt_item (receipt_id);
create index idx_stock_receipt_item_item_sku on stock_receipt_item (item_stock_sku);
create index idx_stock_receipt_item_ingredient_sku on stock_receipt_item (ingredient_sku);
create index idx_supplier_restaurant on supplier (restaurant_id);
create index idx_supplier_name on supplier (supplier_name);
create index idx_tax_rate_restaurant on tax_rate (restaurant_id);
create index idx_tbl_password_reset_request_email on tbl_password_reset_request (email);
create index idx_tbl_password_reset_request_token on tbl_password_reset_request (token_id);
create index idx_tbl_password_reset_request_status on tbl_password_reset_request (status);
create index idx_tbl_user_email on tbl_user (email);

alter table if exists ingredient_stock add constraint fk_ingredient_stock_supplier foreign key (supplier_id) references supplier;
alter table if exists invoice add constraint fk_invoice_order foreign key (order_id) references orders;
alter table if exists item_stock add constraint fk_item_stock_menu_item foreign key (menu_item_id) references menu_item;
alter table if exists item_stock add constraint fk_item_stock_supplier foreign key (supplier_id) references supplier;
alter table if exists menu_item add constraint fk_menu_item_category foreign key (category_id) references category;
alter table if exists menu_item add constraint fk_menu_item_price foreign key (price_id) references item_price;
alter table if exists menu_item add constraint fk_menu_item_product_image foreign key (product_image_image_id) references product_file;
alter table if exists menu_item_ingredient add constraint fk_menu_item_ingredient_ingredient foreign key (ingredient_sku) references ingredient_stock;
alter table if exists menu_item_ingredient add constraint fk_menu_item_ingredient_menu foreign key (menu_item_id) references menu_item;
alter table if exists menu_item_ingredient add constraint fk_menu_item_ingredient_recipe foreign key (recipe_id) references menu_recipe;
alter table if exists menu_recipe add constraint fk_menu_recipe_menu foreign key (menu_item_id) references menu_item;
alter table if exists order_tax add constraint fk_order_tax_order foreign key (order_id) references orders;
alter table if exists restaurant add constraint fk_restaurant_chain foreign key (chain_id) references restaurant_chain;
alter table if exists sale_item add constraint fk_sale_item_menu_item foreign key (manu_item_id) references menu_item;
alter table if exists sale_item add constraint fk_sale_item_order foreign key (order_id) references orders;
alter table if exists stock_receipt add constraint fk_stock_receipt_supplier foreign key (supplier_id) references supplier;
alter table if exists stock_receipt_item add constraint fk_stock_receipt_item_ingredient foreign key (ingredient_sku) references ingredient_stock;
alter table if exists stock_receipt_item add constraint fk_stock_receipt_item_item_stock foreign key (item_stock_sku) references item_stock;
alter table if exists stock_receipt_item add constraint fk_stock_receipt_item_receipt foreign key (receipt_id) references stock_receipt;
alter table if exists tbl_user_roles add constraint fk_tbl_user_roles_role foreign key (roles_role_id) references user_roles;
alter table if exists tbl_user_roles add constraint fk_tbl_user_roles_user foreign key (user_user_id) references tbl_user;
