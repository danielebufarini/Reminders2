package com.danielebufarini.reminders2.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import static android.util.Patterns.EMAIL_ADDRESS;

public class GoogleAccountHelper {
    private final String NO_ACCOUNT_SETUP = "<no account set up>";
    private Account[] accounts;
    private String[] accountNames;
    private Context context;
    private Map<String, Integer> reverseIndex;

    public GoogleAccountHelper(Context context) {
        this.context = context;
        doGetAccounts();
        accountNames = new String[accounts.length];
        for (int i = 0; i < accounts.length; ++i)
            accountNames[i] = accounts[i].name;
    }

    private void doGetAccounts() {
        Account[] gAccounts = AccountManager.get(context).getAccountsByType("com.google");
        accounts = new Account[gAccounts.length];
        reverseIndex = new HashMap<>(gAccounts.length);
        if (gAccounts.length == 0) {
            accounts = new Account[1];
            accounts[0] = new Account(NO_ACCOUNT_SETUP, "com.danielebufarini");
        } else
            for (int i = 0; i < gAccounts.length; i++) {
                Account account = gAccounts[i];
                if (EMAIL_ADDRESS.matcher(account.name).matches()) {
                    reverseIndex.put(account.name, i);
                    accounts[i++] = account;
                }
            }
    }

    public Account[] getAccounts() {
        return accounts;
    }

    public String[] getNames() {
        return accountNames;
    }

    public int getIndex(String selectedAccountName) {
        return reverseIndex.get(selectedAccountName);
    }
}
