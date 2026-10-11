package bot.wheretoeat;

import bot.wheretoeat.service.RestaurantRepository;
import bot.wheretoeat.service.RestaurantRepositoryImpl;
import bot.wheretoeat.ui.ConsoleApp;

public class Main {

    public static void main(String[] args) {
        RestaurantRepository repository = new RestaurantRepositoryImpl();
        new ConsoleApp(repository).run();
    }
}