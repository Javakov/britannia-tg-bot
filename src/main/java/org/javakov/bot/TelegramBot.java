package org.javakov.bot;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@SuppressWarnings("deprecation")
public class TelegramBot extends TelegramLongPollingBot {
    private static final String BOT_USERNAME = "BRITANNIA_BOT";
    private static final String BOT_TOKEN = "";
    private static final String MENTIONS = "";
    private static final String BOT_HANDLE = "";
    private static final String POLL_YES = "✅ Да";
    private static final String POLL_NO = "❌ Нет";
    private static final String POLL_TYPE = "regular";

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() && !update.hasChannelPost()) return;

        String text = extractText(update);
        String chatId = extractChatId(update);
        Integer messageId = extractMessageId(update);
        String userName = extractUserName(update);

        if (text == null || userName == null || !isCommand(text)) return;

        deleteMessage(chatId, messageId);
        String mentionsWithoutUser = getMentionsWithoutUser(userName);

        handleCommand(text, chatId, userName, mentionsWithoutUser);
    }

    private String extractText(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return update.getMessage().getText().toLowerCase().trim();
        } else if (update.hasChannelPost() && update.getChannelPost().hasText()) {
            return update.getChannelPost().getText().toLowerCase().trim();
        }
        return null;
    }

    private String extractChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId().toString();
        } else if (update.hasChannelPost()) {
            return update.getChannelPost().getChatId().toString();
        }
        return null;
    }

    private Integer extractMessageId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getMessageId();
        } else if (update.hasChannelPost()) {
            return update.getChannelPost().getMessageId();
        }
        return null;
    }

    private String extractUserName(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getUserName();
        } else if (update.hasChannelPost()) {
            return update.getChannelPost().getFrom().getUserName();
        }
        return null;
    }

    private boolean isCommand(String text) {
        return text.equals("/p" + BOT_HANDLE)
                || text.equals("/g" + BOT_HANDLE)
                || text.startsWith("/i" + BOT_HANDLE)
                || text.startsWith("/all" + BOT_HANDLE);
    }

    private String getMentionsWithoutUser(String userName) {
        return MENTIONS.replace("@" + userName, "").trim();
    }

    private void handleCommand(String text, String chatId, String userName, String mentionsWithoutUser) {
        if (text.equals("/p" + BOT_HANDLE)) {
            sendPoll(chatId, "Гайс, го покур! 🚬\n\nОт британца @" + userName, List.of(POLL_YES, POLL_NO));
        } else if (text.equals("/g" + BOT_HANDLE)) {
            sendPoll(chatId, "Гайс, выходим гулять! 🌳\n\nОт британца @" + userName, List.of(POLL_YES, POLL_NO));
        } else if (text.equals("/all" + BOT_HANDLE)) {
            sendMessage(chatId, "\uD83D\uDD0AГайс, внимание!\uD83D\uDD0A\n\n" + mentionsWithoutUser + "\n\nОт британца @" + userName);
        } else if (text.startsWith("/i" + BOT_HANDLE)) {
            String game = text.substring(23).trim();
            if (!game.isEmpty()) {
                sendPoll(chatId, "Гайс, го в " + game + "! 🎮\n\nОт британца @" + userName, List.of(POLL_YES, POLL_NO));
            }
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn("Ошибка отправки сообщения", e);
        }
    }

    private void sendPoll(String chatId, String question, List<String> options) {
        SendPoll sendPoll = new SendPoll();
        sendPoll.setChatId(chatId);
        sendPoll.setQuestion(question);
        sendPoll.setOptions(options);
        sendPoll.setType(POLL_TYPE);
        sendPoll.setIsAnonymous(false);
        try {
            execute(sendPoll);
        } catch (TelegramApiException e) {
            log.warn("Ошибка отправки голосования", e);
        }
    }

    private void deleteMessage(String chatId, Integer messageId) {
        if (messageId == null) return;
        DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.warn("Ошибка удаления сообщения", e);
        }
    }
}
