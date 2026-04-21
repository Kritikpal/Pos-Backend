alter table if exists configured_menu_option
    add column if not exists min_quantity integer;

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_name = 'configured_menu_option'
          and column_name = 'default_quantity'
    ) then
        execute 'update configured_menu_option set min_quantity = coalesce(min_quantity, default_quantity)';
        execute 'alter table configured_menu_option drop column if exists default_quantity';
    end if;
end
$$;
