package ru.muiv.diplom.domain;

public enum AccountType {
    CHECKING("Текущий"),
    SAVINGS("Сберегательный");

    private final String title;

    AccountType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
