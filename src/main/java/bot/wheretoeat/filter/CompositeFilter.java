package bot.wheretoeat.filter;

import bot.wheretoeat.model.Restaurant;

import java.util.Arrays;
import java.util.List;

public class CompositeFilter implements Filter {
    private final List<Filter> filters;

    public CompositeFilter(Filter... filters) {
        this.filters = Arrays.asList(filters);
    }

    @Override
    public boolean matches(Restaurant restaurant) {
        return filters.stream().allMatch(f -> f.matches(restaurant));
    }
}
