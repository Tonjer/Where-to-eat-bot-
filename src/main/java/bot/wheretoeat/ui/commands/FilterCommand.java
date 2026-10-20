package bot.wheretoeat.ui.commands;

import bot.wheretoeat.service.RestaurantRepository;
import bot.wheretoeat.ui.FilterMenu;
import bot.wheretoeat.ui.ConsoleReader;
import bot.wheretoeat.ui.MenuPrinter;

public class FilterCommand implements Command {

    private final FilterMenu filterMenu;

    public FilterCommand(RestaurantRepository repository, MenuPrinter printer) {
        this.filterMenu = new FilterMenu(repository, printer);
    }
    @Override
    public void execute() {
        filterMenu.show();

        while (true) {
            System.out.print("(фильтры) > ");
            String input = ConsoleReader.readLine();

            if (input.equals("0")) {
                return;
            }
            filterMenu.handleInput(input);
        }
    }
}