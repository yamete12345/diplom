package ru.muiv.diplom.domain;

public enum TransactionType {
    DEPOSIT("Пополнение"),
    WITHDRAW("Списание"),
    TRANSFER("Перевод");

    private final String title;

    TransactionType(String title) {
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
