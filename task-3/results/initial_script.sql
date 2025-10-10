create table user_data
(
    id              serial primary key,
    name            text,
    login           text,
    additional_data text
);

insert into user_data(name, login, additional_data)
values
    ('Дмитрий', 'dmitry', 'Данные'),
    ('Антон', 'anton', 'Данные'),
    ('Иннокентий', 'innokenty', 'Данные'),
    ('Евгений', 'evgeny', 'Данные'),
    ('Максим', 'max', 'Данные'),
    ('Олег', 'oleg', 'Данные'),
    ('Александр', 'alex', 'Данные'),
    ('Борис', 'boris', 'Данные'),
    ('Игорь', 'igor', 'Данные'),
    ('Егор', 'egor', 'Данные')