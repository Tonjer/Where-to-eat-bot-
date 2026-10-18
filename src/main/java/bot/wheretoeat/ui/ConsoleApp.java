
package bot.wheretoeat.ui;

import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.ui.commands.Command;
import bot.wheretoeat.ui.commands.ExitCommand;
import bot.wheretoeat.ui.commands.FilterCommand;
import bot.wheretoeat.ui.commands.HelpCommand;
import bot.wheretoeat.ui.commands.NotImplementedCommand;

import java.util.HashMap;
import java.util.Map;

public class ConsoleApp {

    private final MenuPrinter menuPrinter = new MenuPrinter();
    private final Map<String, Command> commands = new HashMap<>();

    public ConsoleApp(RestaurantService service) {
        commands.put("1", new FilterCommand(service, menuPrinter));
        commands.put("2", new NotImplementedCommand());
        commands.put("3", new NotImplementedCommand());
        commands.put("/help", new HelpCommand());
        commands.put("help", new HelpCommand());
        commands.put("/exit", new ExitCommand());
        commands.put("exit", new ExitCommand());
    }

    public void run() {
        menuPrinter.printWelcome();
        menuPrinter.printMainMenu();

        while (true) {
            System.out.print("> ");
            String input = ConsoleReader.readLine();

            if (input.isEmpty()) {
                continue;
            }

            Command command = commands.get(input.toLowerCase());

            if (command == null) {
                System.out.println(
                        "Неизвестная команда. Введите 1-3 или /exit для выхода."
                );
                menuPrinter.printMainMenu();
                continue;
            }

            command.execute();

            if (command instanceof ExitCommand) {
                return;
            }
        }
    }
}
