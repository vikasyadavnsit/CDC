package com.vikasyadavnsit.cdc.filter;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Pattern;

public class RegexInputFilter implements InputFilter {

    private String regex = "[a-zA-Z0-9\\s-_]+";

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String newText = dest.subSequence(0, dstart).toString()
                + source.subSequence(start, end).toString()
                + dest.subSequence(dend, dest.length()).toString();
        return Pattern.compile(regex).matcher(newText).matches() ? null : "";
    }
}
