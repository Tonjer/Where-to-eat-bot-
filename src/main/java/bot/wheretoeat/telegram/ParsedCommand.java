package bot.wheretoeat.telegram;

import java.util.Locale;

public class ParsedCommand {

    private final String rawText;
    private final String command;
    private final String targetBot;
    private final String arguments;

    private ParsedCommand(String rawText, String command, String targetBot, String arguments) {
        this.rawText = rawText;
        this.command = command;
        this.targetBot = targetBot;
        this.arguments = arguments;
    }

    public static ParsedCommand parse(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("/")) {
            return new ParsedCommand(trimmed, null, null, trimmed);
        }
        String[] parts = trimmed.split("\\s+", 2);
        String token = parts[0];
        String args = parts.length > 1 ? parts[1].trim() : "";

        int atIdx = token.indexOf("@");
        String cmdName;
        String botName = null;
        if (atIdx >= 0){
            cmdName = token.substring(0, atIdx).toLowerCase();
            botName = token.substring(atIdx + 1).trim();
        } else {
            cmdName = token.toLowerCase();
        }
        return new ParsedCommand(trimmed, cmdName, botName, args);
    }
    public boolean isCommand() { return command != null; }

    public boolean isAddressedToOtherBot(String ourBotUsername) {
        if (targetBot == null || targetBot.isEmpty() || ourBotUsername == null || ourBotUsername.isBlank()) {
            return false;
        }
        return !targetBot.equalsIgnoreCase(ourBotUsername.trim());
    }

    public String getRawText() { return rawText; }
    public String getCommand() { return command; }
    public String getArguments() { return arguments; }
}
