package bot.wheretoeat.ui.commands;

import bot.wheretoeat.service.commands.Command;

public class NotImplementedCommand implements ConsoleCommand {

    @Override
    public void execute() {
        System.out.println("🚧 Раздел находится в разработке.");
    }
}