package io.dpm.dropmenote.ws.utils;

import java.util.regex.Pattern;

public class HateCheckerUtil {
    // Define a list of popular swear words and their variations
    private static final String[] SWEAR_WORDS = {
            "fuck",
            "fker",
            "fck",
            "f\\*\\*k",
            "shit",
            "sht",
            "sit",
            "sh\\!t",
            "asshole",
            "a\\*\\*hole",
            "ahole",
            "a-hole",
            "as\\$hole",
            "ahole",
            "bitch",
            "b\\*tch",
            "b\\!tch",
            "dick",
            "d\\*ck",
            "d\\!ck",
            "motherfucker",
            "motherfker",
            "motherf\\*\\*ker",
            "pussy",
            "pu\\*\\*y",
            "pus\\$y",
            "pu\\$sy",
            "puy",
            "cock",
            "cOck",
            "cck",
            "c0ck",
            "ass",
            "as\\$",
            "a\\$s",
            "a\\$\\$",
            "as",
            "as",
            "a\\*\\*",
            "cunt",
            "cnt",
            "ct",
            "bastard",
            "twat",
            "wanker",
            "prick",
            "prck",
            "pr\\!ck",
            "bollocks",
            "Douchebag",
            "doucheb@g",
            "piča",
            "pča",
            "hovno",
            "hno",
            "hOvno",
            "hovnO",
            "hovn0",
            "h0vno",
            "kokot",
            "k0k0t",
            "kkt",
            "kokot",
            "debil",
            "debl",
            "deb\\!l",
            "kretén",
            "kurva",
            "krva",
            "krvenec",
            "k\\*\\*venec",
            "suka",
            "ska"
    };

    // Define a static regex pattern
    private static final Pattern PATTERN;

    // Initialize the static pattern in a static block
    static {
        String patternString = "\\b(" + String.join("|", escapeSpecialCharacters()) + ")(s|ed|ing|er)?\\b";
        PATTERN = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }

    // Escape special characters in an array of strings
    private static String[] escapeSpecialCharacters() {
        String[] escapedWords = new String[HateCheckerUtil.SWEAR_WORDS.length];
        for (int i = 0; i < HateCheckerUtil.SWEAR_WORDS.length; i++) {
            escapedWords[i] = Pattern.quote(HateCheckerUtil.SWEAR_WORDS[i]);
        }
        return escapedWords;
    }

    public static boolean checkHate(final String text) {
        // Search for the pattern in the input text
        return PATTERN.matcher(text).find();
    }
}