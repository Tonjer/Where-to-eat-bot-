package bot.wheretoeat.ui.commands;

public class HelpCommand implements Command {

    @Override
    public void execute() {
        System.out.println("Доступные команды:");
        System.out.println("  1      — 🍕 Фильтры: тип еды, кухня, цена");
        System.out.println("  2      — ⭐ Рейтинги и отзывы (в разработке)");
        System.out.println("  3      — 📜 История посещений (в разработке)");
        System.out.println("  /help  — эта справка");
        System.out.println("  /exit  — выход из программы");
        System.out.println("Внутри фильтров: выбирайте пункты цифрами, 0 — назад.");
    }
}