package com.dysi.AngryBoxClientManager.dtos;

public final class ParameterViews {
    private ParameterViews() { }
    public static class BankView {
        public Long id;
        public String name;
        public boolean active;
    }
    public static class BankAccountView {
        public Long id, bankId;
        public String bankName, holder, cbu, alias;
        public boolean active, bankActive, available;
    }
}
