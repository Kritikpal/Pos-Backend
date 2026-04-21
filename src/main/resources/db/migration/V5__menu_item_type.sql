alter table if exists menu_item
    add column if not exists menu_type varchar(30);

update menu_item
set menu_type = case
    when exists (
        select 1
        from configured_menu_template cmt
        where cmt.parent_menu_item_id = menu_item.id
          and cmt.is_deleted = false
    ) then 'CONFIGURABLE'
    when coalesce(is_prepared, false) = true then 'PREPARED'
    when coalesce(has_recipi, false) = true then 'RECIPE'
    else 'DIRECT'
end
where menu_type is null;

alter table if exists menu_item
    alter column menu_type set default 'DIRECT';

update menu_item
set menu_type = 'DIRECT'
where menu_type is null;

alter table if exists menu_item
    alter column menu_type set not null;

create index if not exists idx_menu_item_type on menu_item (menu_type);
