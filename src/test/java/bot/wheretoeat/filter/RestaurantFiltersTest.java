package bot.wheretoeat.filter;

import bot.wheretoeat.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RestaurantFiltersTest {
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant("1", "Tokyo", "Main street 1", "Japanese", List.of("Ramen", "Sushi"), 1200);
    }

    @Test
    void cuisineFilterMatchesIgnoringCase() {
        assertTrue(new CuisineFilter("jApAn").matches(restaurant));
        assertFalse(new CuisineFilter("Italian").matches(restaurant));
    }

    @Test
    void emptyCuisineFilterMatchesAnyRestaurant() {
        assertTrue(new CuisineFilter("  ").matches(restaurant));
        assertTrue(new CuisineFilter(null).matches(restaurant));
    }

    @Test
    void dishFilterFindsPartialDishNameIgnoringCase() {
        assertTrue(new DishFilter("ram").matches(restaurant));
        assertTrue(new DishFilter("SUSHI").matches(restaurant));
        assertFalse(new DishFilter("Pizza").matches(restaurant));
    }

    @Test
    void emptyDishFilterMatchesAnyRestaurant() {
        assertTrue(new DishFilter(null).matches(restaurant));
        assertTrue(new DishFilter(" ").matches(restaurant));
    }

    @Test
    void priceFilterChecksInclusiveBounds() {
        assertTrue(new PriceFilter(1200, 1200).matches(restaurant));
        assertTrue(new PriceFilter(1000, 1500).matches(restaurant));
        assertFalse(new PriceFilter(1201, 2000).matches(restaurant));
        assertFalse(new PriceFilter(0, 1199).matches(restaurant));
    }

    @Test
    void zeroPriceBoundsAreIgnoredAndNegativeBoundsAreNormalized() {
        assertTrue(new PriceFilter(0, 0).matches(restaurant));
        assertTrue(new PriceFilter(-10, -1).matches(restaurant));
    }

    @Test
    void compositeFilterRequiresEveryFilterToMatch() {
        Filter matching = new CompositeFilter(
                new CuisineFilter("Japan"),
                new DishFilter("ramen"),
                new PriceFilter(1000, 1500));
        Filter notMatching = new CompositeFilter(
                new CuisineFilter("Japan"),
                new DishFilter("pizza"),
                new PriceFilter(1000, 1500));

        assertTrue(matching.matches(restaurant));
        assertFalse(notMatching.matches(restaurant));
    }

    @Test
    void emptyCompositeFilterMatchesEverything() {
        assertTrue(new CompositeFilter().matches(restaurant));
    }
}
