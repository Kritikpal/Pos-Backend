create sequence if not exists unit_master_seq start with 1 increment by 50;
create sequence if not exists item_unit_conversion_seq start with 1 increment by 50;

create table if not exists unit_master (
    active boolean not null,
    created_at timestamp(6) not null,
    id bigint not null,
    updated_at timestamp(6) not null,
    code varchar(50) not null,
    display_name varchar(100) not null,
    primary key (id),
    constraint uk_unit_master_code unique (code)
);

create table if not exists ingredient (
    is_active boolean not null,
    is_deleted boolean not null,
    base_unit_id bigint not null,
    created_at timestamp(6) not null,
    restaurant_id bigint not null,
    supplier_id bigint,
    updated_at timestamp(6) not null,
    description text,
    category varchar(120),
    ingredient_name varchar(255) not null,
    sku varchar(255) not null,
    primary key (sku)
);

create table if not exists item_unit_conversion (
    active boolean not null,
    factor_to_base numeric(19,6) not null,
    purchase_allowed boolean not null,
    sale_allowed boolean not null,
    created_at timestamp(6) not null,
    id bigint not null,
    restaurant_id bigint not null,
    unit_id bigint not null,
    updated_at timestamp(6) not null,
    source_type varchar(30) not null,
    source_id varchar(255) not null,
    primary key (id),
    constraint uk_item_unit_conversion_source_unit unique (restaurant_id, source_type, source_id, unit_id)
);

create index if not exists idx_unit_master_code on unit_master (code);
create index if not exists idx_ingredient_restaurant on ingredient (restaurant_id);
create index if not exists idx_ingredient_name on ingredient (ingredient_name);
create index if not exists idx_item_unit_conversion_source on item_unit_conversion (restaurant_id, source_type, source_id);
create index if not exists idx_item_unit_conversion_source_active on item_unit_conversion (restaurant_id, source_type, source_id, active);

alter table if exists menu_item add column if not exists base_unit_id bigint;
alter table if exists stock_receipt_item add column if not exists entered_qty float(53);
alter table if exists stock_receipt_item add column if not exists unit_id bigint;

alter table if exists stock_receipt alter column total_quantity type float(53) using total_quantity::float8;
alter table if exists stock_receipt_item alter column quantity_received type float(53) using quantity_received::float8;

insert into unit_master (id, active, created_at, updated_at, code, display_name)
select nextval('unit_master_seq'),
       true,
       now(),
       now(),
       upper(trim(unit_code)),
       upper(trim(unit_code))
from (
    select distinct unit_of_measure as unit_code
    from ingredient_stock
    where unit_of_measure is not null and trim(unit_of_measure) <> ''
    union
    select distinct unit_of_measure as unit_code
    from item_stock
    where unit_of_measure is not null and trim(unit_of_measure) <> ''
) units
where not exists (
    select 1
    from unit_master um
    where upper(um.code) = upper(trim(units.unit_code))
);

insert into unit_master (id, active, created_at, updated_at, code, display_name)
select nextval('unit_master_seq'),
       true,
       now(),
       now(),
       'UNIT',
       'UNIT'
where not exists (
    select 1 from unit_master where code = 'UNIT'
);

insert into ingredient (sku, ingredient_name, description, category, restaurant_id, supplier_id, base_unit_id, is_active, is_deleted, created_at, updated_at)
select s.sku,
       s.ingredient_name,
       s.description,
       nullif(s.category, ''),
       s.restaurant_id,
       s.supplier_id,
       coalesce(um.id, fallback_um.id),
       s.is_active,
       s.is_deleted,
       coalesce(s.created_at, now()),
       coalesce(s.updated_at, coalesce(s.created_at, now()))
from ingredient_stock s
left join unit_master um on upper(um.code) = upper(s.unit_of_measure)
join unit_master fallback_um on fallback_um.code = 'UNIT'
where not exists (
    select 1 from ingredient i where i.sku = s.sku
);

update menu_item m
set base_unit_id = coalesce(um.id, fallback_um.id)
from item_stock s
left join unit_master um on upper(um.code) = upper(s.unit_of_measure)
join unit_master fallback_um on fallback_um.code = 'UNIT'
where s.menu_item_id = m.id
  and m.base_unit_id is null;

insert into item_unit_conversion (id, restaurant_id, source_type, source_id, unit_id, factor_to_base, purchase_allowed, sale_allowed, active, created_at, updated_at)
select nextval('item_unit_conversion_seq'),
       i.restaurant_id,
       'INGREDIENT',
       i.sku,
       i.base_unit_id,
       1.000000,
       true,
       true,
       true,
       now(),
       now()
from ingredient i
where not exists (
    select 1
    from item_unit_conversion c
    where c.restaurant_id = i.restaurant_id
      and c.source_type = 'INGREDIENT'
      and c.source_id = i.sku
      and c.unit_id = i.base_unit_id
);

insert into item_unit_conversion (id, restaurant_id, source_type, source_id, unit_id, factor_to_base, purchase_allowed, sale_allowed, active, created_at, updated_at)
select nextval('item_unit_conversion_seq'),
       s.restaurant_id,
       'DIRECT_ITEM',
       cast(m.id as varchar(255)),
       m.base_unit_id,
       1.000000,
       true,
       true,
       true,
       now(),
       now()
from item_stock s
join menu_item m on m.id = s.menu_item_id
where m.base_unit_id is not null
  and not exists (
      select 1
      from item_unit_conversion c
      where c.restaurant_id = s.restaurant_id
        and c.source_type = 'DIRECT_ITEM'
        and c.source_id = cast(m.id as varchar(255))
        and c.unit_id = m.base_unit_id
  );

update stock_receipt_item
set entered_qty = quantity_received
where entered_qty is null;

alter table if exists ingredient add constraint fk_ingredient_supplier foreign key (supplier_id) references supplier;
alter table if exists ingredient add constraint fk_ingredient_base_unit foreign key (base_unit_id) references unit_master;
alter table if exists menu_item add constraint fk_menu_item_base_unit foreign key (base_unit_id) references unit_master;
alter table if exists item_unit_conversion add constraint fk_item_unit_conversion_unit foreign key (unit_id) references unit_master;
alter table if exists stock_receipt_item add constraint fk_stock_receipt_item_unit foreign key (unit_id) references unit_master;
