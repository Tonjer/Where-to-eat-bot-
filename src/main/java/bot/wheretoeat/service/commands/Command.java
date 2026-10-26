package bot.wheretoeat.service.commands;

@FunctionalInterface //указываю, что интерфейс имеет только 1 абстрактнй метод
public interface Command<T> { //обобщённый тип T, чтобы команда могла возвращать результат, т.к. в тг нужно отвечать пользователю
    T execute();
}