alter table if exists configured_menu_slot
    add column if not exists min_selections integer;

alter table if exists configured_menu_slot
    add column if not exists max_selections integer;

alter table if exists configured_menu_slot
    drop column if exists selection_mode;

alter table if exists configured_menu_slot
    drop column if exists min_quantity;

alter table if exists configured_menu_slot
    drop column if exists max_quantity;

alter table if exists configured_menu_option
    add column if not exists default_quantity integer;

alter table if exists configured_menu_option
    drop column if exists min_item_quantity;

alter table if exists configured_menu_option
    drop column if exists max_item_quantity;

alter table if exists configured_sale_item_selection
    add column if not exists quantity integer default 1;

update configured_sale_item_selection
set quantity = 1
where quantity is null;

alter table if exists configured_sale_item_selection
    alter column quantity set default 1;

alter table if exists configured_sale_item_selection
    alter column quantity set not null;
