alter table if exists menu_item
    add column if not exists is_prepared boolean not null default false;

create sequence if not exists production_entry_seq start with 1 increment by 50;
create sequence if not exists production_entry_item_seq start with 1 increment by 50;

create table if not exists prepared_item_stock (
    menu_item_id bigint not null,
    restaurant_id bigint not null,
    available_qty float(53) not null,
    reserved_qty float(53) not null,
    unit_code varchar(30) not null,
    is_active boolean not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    primary key (menu_item_id)
);

create table if not exists production_entry (
    id bigint not null,
    restaurant_id bigint not null,
    menu_item_id bigint not null,
    produced_qty float(53) not null,
    unit_code varchar(30) not null,
    recipe_batch_id bigint,
    production_time timestamp(6) not null,
    notes text,
    created_by bigint,
    created_at timestamp(6) not null,
    primary key (id)
);

create table if not exists production_entry_item (
    id bigint not null,
    production_entry_id bigint not null,
    ingredient_sku varchar(100) not null,
    ingredient_name varchar(255) not null,
    deducted_qty float(53) not null,
    unit_code varchar(30) not null,
    primary key (id)
);

create index if not exists idx_prepared_stock_restaurant on prepared_item_stock (restaurant_id);
create index if not exists idx_prepared_stock_menu_item on prepared_item_stock (menu_item_id);
create index if not exists idx_production_entry_restaurant on production_entry (restaurant_id);
create index if not exists idx_production_entry_menu_item on production_entry (menu_item_id);
create index if not exists idx_production_entry_time on production_entry (production_time);
create index if not exists idx_production_entry_item_entry on production_entry_item (production_entry_id);
create index if not exists idx_production_entry_item_sku on production_entry_item (ingredient_sku);

alter table if exists prepared_item_stock
    add constraint fk_prepared_item_stock_menu_item
    foreign key (menu_item_id) references menu_item;

alter table if exists production_entry
    add constraint fk_production_entry_menu_item
    foreign key (menu_item_id) references menu_item;

alter table if exists production_entry_item
    add constraint fk_production_entry_item_entry
    foreign key (production_entry_id) references production_entry;

alter table if exists production_entry_item
    add constraint fk_production_entry_item_ingredient
    foreign key (ingredient_sku) references ingredient_stock;
