package bot.wheretoeat.service.commands;

import bot.wheretoeat.service.RestaurantService;

import java.util.List;

public class GetAllDishesCommand implements Command<List<String>> {
    private final RestaurantService service;

    public GetAllDishesCommand(RestaurantService service) {
        this.service = service;
    }

    @Override
    public List<String> execute() {
        return service.getAllDishes();
    }
}
