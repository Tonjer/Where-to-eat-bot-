package bot.wheretoeat.ui.commands;

public class ExitCommand implements Command {

    @Override
    public void execute() {
        System.out.println("До свидания! Приятного аппетита!");
    }
}