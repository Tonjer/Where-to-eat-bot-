package bot.wheretoeat.model;

import java.util.List;
import java.util.ArrayList;

public class Restaurant {

    private String id;
    private String name;
    private String address;
    private String cuisine;
    private List<String> dishes;
    private int averageCheck;

    public Restaurant() {
    }

    public Restaurant(String id, String name, String address, String cuisine, List<String> dishes, int averageCheck) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.cuisine = cuisine;
        this.dishes = dishes != null ? dishes : new ArrayList<>();
        this.averageCheck = averageCheck;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public List<String> getDishes() {
        return dishes;
    }

    public void setDishes(List<String> dishes) {
        this.dishes = dishes != null ? dishes : new ArrayList<>();
    }

    public int getAverageCheck() {
        return averageCheck;
    }

    public void setAverageCheck(int averageCheck) {
        this.averageCheck = averageCheck;
    }

    @Override
    public String toString() {
        return "🍴 " + name +
                "\n📍 Адрес: " + address +
                "\n🌍 Кухня: " + cuisine +
                "\n🍽 Блюда: " + String.join(", ", dishes) +
                "\n💰 Средний чек: " + averageCheck + " ₽";
    }
}