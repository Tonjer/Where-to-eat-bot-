package bot.wheretoeat.telegram;

@FunctionalInterface
public interface TelegramCommand {
    String execute(long chatId, String text);
}
