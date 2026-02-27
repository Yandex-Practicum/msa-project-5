create table clients
(
  id serial primary key,
  name varchar(100) not null,
  email varchar(100) not null
);

insert into clients (id, name, email)
values (1, 'ООО Логистика', 'info@logistics.ru');

create table drivers
(
  id serial primary key,
  name varchar(100) not null,
  license_number varchar(100) not null
);

insert into drivers (id, name, license_number)
values (1, 'Иван Петров', 'AB123456');

create table vehicles
(
  id serial primary key,
  license_plate varchar(50) not null,
  model varchar(100) not null
);

insert into vehicles (id, license_plate, model)
values (1, 'А123БВ777', 'Газель Next');

create table shipments
(
  id serial primary key,
  driver_id integer not null,
  origin varchar(100) not null,
  destination varchar(100) not null,
  amount decimal(15,2) not null
);

insert into shipments (id, driver_id, origin, destination, amount)
values (1, 1, 'Москва', 'Тверь',    50000.00),
       (2, 1, 'СПб',    'Новгород',   40000.00),
       (3, 1, 'СПб',    'Москва', 35000.00);

create table shipment_events
(
  id serial primary key,
  shipment_id integer not null,
  event_type varchar(100) not null
);

insert into shipment_events (id, shipment_id, event_type)
values (1, 1, 'created');