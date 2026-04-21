alter table if exists menu_item
    add column if not exists item_version bigint;

update menu_item
set item_version = 0
where item_version is null;

alter table if exists menu_item
    alter column item_version set default 0;

alter table if exists menu_recipe
    add column if not exists recipe_version integer;

update menu_recipe
set recipe_version = 0
where recipe_version is null;

alter table if exists menu_recipe
    alter column recipe_version set default 0;

update configured_menu_template
set version = 0
where version is null;

alter table if exists configured_menu_template
    alter column version set default 0;

update configured_menu_slot
set version = 0
where version is null;

alter table if exists configured_menu_slot
    alter column version set default 0;

update configured_menu_option
set version = 0
where version is null;

alter table if exists configured_menu_option
    alter column version set default 0;

update configured_sale_item
set version = 0
where version is null;

alter table if exists configured_sale_item
    alter column version set default 0;

update configured_sale_item_selection
set version = 0
where version is null;

alter table if exists configured_sale_item_selection
    alter column version set default 0;
