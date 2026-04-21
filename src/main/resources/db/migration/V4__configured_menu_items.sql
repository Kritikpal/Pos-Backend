create sequence if not exists configured_menu_template_seq start with 1 increment by 50;
create sequence if not exists configured_menu_slot_seq start with 1 increment by 50;
create sequence if not exists configured_menu_option_seq start with 1 increment by 50;
create sequence if not exists configured_sale_item_seq start with 1 increment by 50;
create sequence if not exists configured_sale_item_selection_seq start with 1 increment by 50;

create table if not exists configured_menu_template (
    id bigint not null,
    parent_menu_item_id bigint not null unique,
    restaurant_id bigint not null,
    is_active boolean not null,
    is_deleted boolean not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version bigint,
    primary key (id)
);

create table if not exists configured_menu_slot (
    id bigint not null,
    template_id bigint not null,
    slot_key varchar(100) not null,
    slot_name varchar(255) not null,
    min_selections integer not null,
    max_selections integer not null,
    display_order integer not null,
    is_required boolean not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version bigint,
    primary key (id)
);

create table if not exists configured_menu_option (
    id bigint not null,
    slot_id bigint not null,
    child_menu_item_id bigint not null,
    price_delta float(53) not null,
    display_order integer not null,
    is_default boolean not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version bigint,
    primary key (id),
    constraint uk_configured_menu_slot_option unique (slot_id, child_menu_item_id)
);

create table if not exists configured_sale_item (
    id bigint not null,
    order_id bigint not null,
    configured_template_id bigint not null,
    parent_menu_item_id bigint not null,
    line_name varchar(255) not null,
    base_price float(53) not null,
    unit_price float(53) not null,
    amount integer not null,
    restaurant_id bigint not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version bigint,
    primary key (id)
);

create table if not exists configured_sale_item_selection (
    id bigint not null,
    configured_sale_item_id bigint not null,
    slot_id bigint not null,
    slot_name varchar(255) not null,
    child_menu_item_id bigint not null,
    child_item_name varchar(255) not null,
    price_delta float(53) not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    version bigint,
    primary key (id)
);

create index if not exists idx_configured_menu_template_restaurant on configured_menu_template (restaurant_id);
create index if not exists idx_configured_menu_template_parent on configured_menu_template (parent_menu_item_id);
create index if not exists idx_configured_menu_slot_template on configured_menu_slot (template_id);
create index if not exists idx_configured_menu_slot_order on configured_menu_slot (display_order);
create index if not exists idx_configured_menu_option_slot on configured_menu_option (slot_id);
create index if not exists idx_configured_menu_option_child on configured_menu_option (child_menu_item_id);
create index if not exists idx_configured_sale_item_order on configured_sale_item (order_id);
create index if not exists idx_configured_sale_item_template on configured_sale_item (configured_template_id);
create index if not exists idx_configured_sale_item_restaurant on configured_sale_item (restaurant_id);
create index if not exists idx_configured_sale_item_selection_item on configured_sale_item_selection (configured_sale_item_id);

alter table if exists configured_menu_template
    add constraint fk_configured_menu_template_parent
    foreign key (parent_menu_item_id) references menu_item;

alter table if exists configured_menu_slot
    add constraint fk_configured_menu_slot_template
    foreign key (template_id) references configured_menu_template;

alter table if exists configured_menu_option
    add constraint fk_configured_menu_option_slot
    foreign key (slot_id) references configured_menu_slot;

alter table if exists configured_menu_option
    add constraint fk_configured_menu_option_child
    foreign key (child_menu_item_id) references menu_item;

alter table if exists configured_sale_item
    add constraint fk_configured_sale_item_order
    foreign key (order_id) references orders;

alter table if exists configured_sale_item
    add constraint fk_configured_sale_item_template
    foreign key (configured_template_id) references configured_menu_template;

alter table if exists configured_sale_item
    add constraint fk_configured_sale_item_parent_menu
    foreign key (parent_menu_item_id) references menu_item;

alter table if exists configured_sale_item_selection
    add constraint fk_configured_sale_item_selection_item
    foreign key (configured_sale_item_id) references configured_sale_item;

alter table if exists configured_sale_item_selection
    add constraint fk_configured_sale_item_selection_child_menu
    foreign key (child_menu_item_id) references menu_item;
