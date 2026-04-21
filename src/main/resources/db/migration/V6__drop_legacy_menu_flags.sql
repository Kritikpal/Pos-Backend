update menu_item m
set menu_type = case
    when exists (
        select 1
        from configured_menu_template cmt
        where cmt.parent_menu_item_id = m.id
          and cmt.is_deleted = false
    ) then 'CONFIGURABLE'
    when coalesce(m.is_prepared, false) = true then 'PREPARED'
    when coalesce(m.has_recipi, false) = true then 'RECIPE'
    else coalesce(m.menu_type, 'DIRECT')
end;

alter table if exists menu_item
    drop column if exists is_prepared;

alter table if exists menu_item
    drop column if exists has_recipi;
