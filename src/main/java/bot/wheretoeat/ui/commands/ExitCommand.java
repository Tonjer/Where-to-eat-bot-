package bot.wheretoeat.ui.commands;


public class ExitCommand implements ConsoleCommand {

    @Override
    public void execute() {
        System.out.println("До свидания! Приятного аппетита!");
    }
}