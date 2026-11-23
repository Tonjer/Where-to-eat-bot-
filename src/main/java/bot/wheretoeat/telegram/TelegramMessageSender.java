package bot.wheretoeat.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.List;

public class TelegramMessageSender {

    private static final int MAX_CHUNK_SIZE = 3800;

    private final TelegramLongPollingBot bot;

    public TelegramMessageSender(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public boolean sendMessage(long chatId, String text) {
        return sendMessage(chatId, text, null);
    }

    public boolean sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard) {
        if (text == null || text.isBlank()) {
            return false;
        }
        List<String> parts = splitLongMessage(text, MAX_CHUNK_SIZE);
        boolean success = true;

        for (int i = 0; i < parts.size(); i++) {
            SendMessage message = new SendMessage(String.valueOf(chatId), parts.get(i));
            if (i == parts.size() - 1 && keyboard != null) {
                message.setReplyMarkup(keyboard);
            }
            if (!executeMessage(message)) {
                success = false;
                break;
            }
        }
        return success;
    }

    private List<String> splitLongMessage(String text, int maxSize) {
        if (text.length() <= maxSize) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline + 1;
                }
            }
            chunks.add(text.substring(start, end).stripTrailing());
            start = end;
        }
        return chunks;
    }

    private boolean executeMessage(SendMessage message) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                bot.execute(message);
                return true;
            } catch (TelegramApiRequestException e) {
                if (e.getErrorCode() != null && e.getErrorCode() == 429 && attempt == 1) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue;
                }
                System.err.println("Ошибка Telegram API при отправке сообщения: " + e.getMessage());
                return false;
            } catch (TelegramApiException e) {
                System.err.println("Не удалось отправить сообщение в Telegram.");
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}