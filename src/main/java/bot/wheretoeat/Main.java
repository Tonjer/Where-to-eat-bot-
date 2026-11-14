package bot.wheretoeat;

import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.InFileRestarauntRepositry;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.ui.ConsoleApp;

public class Main {

    public static void main(String[] args) {
        RestaurantRepository repository = new InFileRestarauntRepositry();
        RestaurantService service = new RestaurantService(repository);
        new ConsoleApp(service).run();
    }
}