package bot.wheretoeat.telegram;

import bot.wheretoeat.repository.RestaurantRepository;
import bot.wheretoeat.repository.InFileRestarauntRepositry;
import bot.wheretoeat.service.RestaurantService;
import bot.wheretoeat.repository.ReviewRepository;
import bot.wheretoeat.repository.InFileReviewRepository;
import bot.wheretoeat.service.ReviewService;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class TelegramMain {

    // TODO(refactor P2): Проверять конфигурацию до создания хранилищ; вынести username и путь данных,
    // убрать требование ровно двух администраторов. Обновить README для реального запуска Telegram.
    public static void main(String[] args) {
        String botToken = System.getenv("Telegram_Bot_Token");

        if (botToken == null || botToken.isBlank()) {
            System.err.println(
                    "Ошибка: не задана переменная окружения "
                            + "Telegram_Bot_Token."
            );
            return;
        }

        String botUsername = "Where_ToEat_bot";

        try {
            RestaurantRepository repository =
                    new InFileRestarauntRepositry();

            RestaurantService service =
                    new RestaurantService(repository);

            ReviewRepository reviewRepository =
                    new InFileReviewRepository();

            ReviewService reviewService =
                    new ReviewService(reviewRepository, repository);

            TelegramHelpHandler helpHandler =
                    new TelegramHelpHandler();

            TelegramRestaurantHandler restaurantHandler =
                    new TelegramRestaurantHandler(service);

            TelegramFilterHandler filterHandler =
                    new TelegramFilterHandler(service, restaurantHandler);

            TelegramReviewHandler reviewHandler =
                    new TelegramReviewHandler(reviewService, service);

            String adminIdsValue = System.getenv("TELEGRAM_ADMIN_IDS");

            if (adminIdsValue == null || adminIdsValue.isBlank()) {
                throw new RuntimeException("Не задана переменная окружения TELEGRAM_ADMIN_IDS");
            }
            Set<Long> adminIds = Arrays.stream(adminIdsValue.split(",")).map(String::trim).map(Long::parseLong).collect(Collectors.toSet());

            if (adminIds.size() != 2) {
                throw new RuntimeException(
                        "Необходимо указать ID ровно двух администраторов"
                );
            }

            TelegramAdminHandler adminHandler =
                    new TelegramAdminHandler(reviewService, adminIds);

            TelegramCommandHandler commandHandler =
                    new TelegramCommandHandler(
                            helpHandler,
                            restaurantHandler,
                            filterHandler,
                            reviewHandler,
                            adminHandler
                    );

            TelegramBot bot = new TelegramBot(
                    botUsername,
                    botToken,
                    commandHandler
            );

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(bot);

            // TODO(refactor P2): Хранить имя, описание и доступ команды рядом с её регистрацией;
            // генерировать меню и /help из одного списка, без внедрения универсального фреймворка команд.
            SetMyCommands setMyCommands = new SetMyCommands();
            setMyCommands.setCommands(List.of(
                    new BotCommand("start", "Запустить бота"),
                    new BotCommand("help", "Справка"),
                    new BotCommand("all", "Все рестораны"),
                    new BotCommand("filters", "Меню фильтров"),
                    new BotCommand("cuisine", "Выбрать кухню"),
                    new BotCommand("dish", "Выбрать блюдо"),
                    new BotCommand("price", "Выбрать средний чек"),
                    new BotCommand("search", "Найти рестораны"),
                    new BotCommand("reviews", "Отзывы о ресторане"),
                    new BotCommand("write_review", "Написать отзыв"),
                    new BotCommand("reset", "Сбросить фильтры"),
                    new BotCommand("cancel", "Отменить выбор"),
                    new BotCommand("admin", "Панель администратора")
            ));
            bot.execute(setMyCommands);
            System.out.println("Telegram-бот запущен.");
            System.out.println("Имя бота: @" + botUsername);

        } catch (TelegramApiException e) {
            System.err.println("Не удалось запустить Telegram-бота.");
            e.printStackTrace();

        } catch (RuntimeException e) {
            System.err.println(
                    "Ошибка при создании приложения "
                            + "или загрузке базы ресторанов."
            );
            e.printStackTrace();
        }
    }
}
