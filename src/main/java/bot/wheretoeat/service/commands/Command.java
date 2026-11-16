package bot.wheretoeat.service.commands;

@FunctionalInterface
public interface Command<T> {
    T execute();
}