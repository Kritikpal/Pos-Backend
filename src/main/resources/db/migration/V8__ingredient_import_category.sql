alter table if exists ingredient_stock
    add column if not exists category varchar(120);
