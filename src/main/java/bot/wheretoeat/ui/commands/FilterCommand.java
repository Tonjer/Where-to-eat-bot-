
package bot.wheretoeat.ui.commands;

import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.ui.ConsoleReader;
import bot.wheretoeat.ui.FilterMenu;
import bot.wheretoeat.ui.MenuPrinter;

public class FilterCommand implements Command {

    private final FilterMenu filterMenu;

    public FilterCommand(
            RestaurantService service,
            MenuPrinter printer
    ) {
        this.filterMenu = new FilterMenu(service, printer);
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
