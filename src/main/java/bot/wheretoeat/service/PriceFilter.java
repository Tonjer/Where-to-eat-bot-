package bot.wheretoeat.service;

import bot.wheretoeat.model.Restaurant;

public class PriceFilter implements Filter {

    private final int minCheck;
    private final int maxCheck;

    public PriceFilter(int minCheck, int maxCheck) {
        this.minCheck = Math.max(0, minCheck);
        this.maxCheck = Math.max(0, maxCheck);
    }

    @Override
    public boolean matches(Restaurant restaurant) {
        if (minCheck > 0 && restaurant.getAverageCheck() < minCheck) {
            return false;
        }
        if (maxCheck > 0 && restaurant.getAverageCheck() > maxCheck) {
            return false;
        }
        return true;
    }
}