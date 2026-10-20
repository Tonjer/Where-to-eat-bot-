package bot.wheretoeat.ui.commands;

public class NotImplementedCommand implements Command {

    @Override
    public void execute() {
        System.out.println("🚧 Раздел находится в разработке.");
    }
}