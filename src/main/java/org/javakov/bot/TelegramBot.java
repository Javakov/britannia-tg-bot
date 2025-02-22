package org.javakov.bot;

import lombok.extern.slf4j.Slf4j;
import org.javakov.config.AppConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("deprecation")
public class TelegramBot extends TelegramLongPollingBot {
    private static final AppConfig CONFIG = new AppConfig();
    private static final String BOT_HANDLE = CONFIG.getBotHandle();
    private static final String POLL_TYPE = "regular";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<String, Set<String>> pollParticipants = new ConcurrentHashMap<>();
    private static Set<String> allUsers = ConcurrentHashMap.newKeySet();

    @Override
    public String getBotUsername() {
        return CONFIG.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return CONFIG.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasPollAnswer()) {
            handlePollAnswer(update.getPollAnswer());
        } else {
            if (!update.hasMessage() && !update.hasChannelPost()) return;

            String text = extractText(update);
            String chatId = extractChatId(update);
            Integer messageId = extractMessageId(update);
            String userName = extractUserName(update);

            if (text == null || userName == null || !isCommand(text)) return;

            deleteMessage(chatId, messageId);
            allUsers = getChatAdministrators(chatId);
            String mentionsWithoutUser = getMentionsWithoutUser(userName, allUsers);
            handleCommand(text, chatId, userName, mentionsWithoutUser);
        }
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
                || text.equals("/s" + BOT_HANDLE)
                || text.startsWith("/i" + BOT_HANDLE)
                || text.equals("/all" + BOT_HANDLE);
    }

    private String getMentionsWithoutUser(String userName, Set<String> allUsers) {
        if (userName == null || userName.trim().isEmpty()) {
            return String.join(" ", allUsers);
        }

        return allUsers.stream()
                .filter(user -> !user.equalsIgnoreCase("@" + userName))
                .collect(Collectors.joining(" "));
    }

    private String getEventTime(int hoursToAdd) {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        ZonedDateTime eventTime = now.plusHours(hoursToAdd);

        return eventTime.format(TIME_FORMATTER);
    }

    private void handleCommand(String text, String chatId, String userName, String mentionsWithoutUser) {
        String lowerText = text.toLowerCase();
        List<String> options = generatePollOptions();

        if (lowerText.equals("/p" + BOT_HANDLE)) {
            String question = "Гайс, го покур! 🚬" +
                    "\n\nОт британца @" + userName;

            sendPollWithReminder(chatId, question, options, allUsers);

        } else if (lowerText.equals("/g" + BOT_HANDLE)) {
            String question = "Гайс, выходим гулять! 🌳" +
                    "\n\nОт британца @" + userName;

            sendPollWithReminder(chatId, question, options, allUsers);

        } else if (lowerText.equals("/s" + BOT_HANDLE)) {
            String question = "Гайс, го собрание! \uD83C\uDDEC\uD83C\uDDE7" +
                    "\n\nОт британца @" + userName;

            sendPollWithReminder(chatId, question, options, allUsers);

        } else if (lowerText.equals("/all" + BOT_HANDLE)) {
            String message = "\uD83D\uDD0AГайс, внимание!\uD83D\uDD0A\n\n" + mentionsWithoutUser +
                    "\n\nОт британца @" + userName;
            sendMessage(chatId, message);

        } else if (lowerText.startsWith("/i" + BOT_HANDLE)) {
            String game = text.substring(("/i" + BOT_HANDLE).length()).trim();
            if (!game.isEmpty()) {
                String question = "Гайс, го в " + game + "! 🎮" +
                        "\n\nОт британца @" + userName;

                sendPollWithReminder(chatId, question, options, allUsers);
            }
        }
    }

    private List<String> generatePollOptions() {
        List<String> options = new ArrayList<>();
        options.add("✅ Да (" + getEventTime(1) + ")");
        options.add("✅ Да (" + getEventTime(2) + ")");
        options.add("✅ Да (" + getEventTime(3) + ")");
        options.add("❓ Да, позже");
        options.add("❌ Нет");

        return options;
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn("Ошибка отправки сообщения", e);
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

    private void sendPollWithReminder(String chatId, String question, List<String> options, Set<String> allUsers) {
        SendPoll sendPoll = new SendPoll(chatId, question, options);
        sendPoll.setChatId(chatId);
        sendPoll.setQuestion(question);
        sendPoll.setOptions(options);
        sendPoll.setType(POLL_TYPE);
        sendPoll.setIsAnonymous(false);

        try {
            String pollId = execute(sendPoll).getPoll().getId();
            pollParticipants.put(pollId, new HashSet<>(allUsers));

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> remindNonVoters(chatId, pollId), 5, TimeUnit.MINUTES);
        } catch (TelegramApiException e) {
            log.warn("Ошибка отправки голосования", e);
        }
    }

    private void handlePollAnswer(PollAnswer pollAnswer) {
        String pollId = pollAnswer.getPollId();
        String userId = pollAnswer.getUser().getUserName();

        pollParticipants.computeIfPresent(pollId, (id, users) -> {
            users.remove("@" + userId);
            return users.isEmpty() ? null : users;
        });
    }

    private void remindNonVoters(String chatId, String pollId) {
        Set<String> remainingUsers = pollParticipants.remove(pollId);
        if (remainingUsers != null && !remainingUsers.isEmpty()) {
            String mentionMessage = "Гайс, не забываем голосовать! ⏳\n\n" + String.join(" ", remainingUsers);
            sendMessage(chatId, mentionMessage);
        }
    }

    private Set<String> getChatAdministrators(String chatId) {
        Set<String> admins = new HashSet<>();
        try {
            List<ChatMember> administrators = execute(new GetChatAdministrators(chatId));
            for (ChatMember admin : administrators) {
                User user = admin.getUser();
                String username = user.getUserName();

                if (username != null && !isBot(username)) {
                    admins.add("@" + username);
                } else if (username == null) {
                    admins.add(user.getFirstName());
                }
            }
        } catch (TelegramApiException e) {
            log.error("Ошибка получения списка администраторов: {}", e.getMessage());
        }

        return admins;
    }

    private boolean isBot(String username) {
        if (username == null || BOT_HANDLE == null) {
            return false;
        }

        String normalizedUsername = username.replaceFirst("^@", "");
        String normalizedBotHandle = BOT_HANDLE.replaceFirst("^@", "");

        return normalizedUsername.equalsIgnoreCase(normalizedBotHandle);
    }
}
