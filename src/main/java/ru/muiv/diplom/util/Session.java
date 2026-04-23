package ru.muiv.diplom.util;

import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.User;

@Component
public class Session {

    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public void clear() {
        currentUser = null;
    }

    public Long requireUserId() {
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        return currentUser.getId();
    }
}
