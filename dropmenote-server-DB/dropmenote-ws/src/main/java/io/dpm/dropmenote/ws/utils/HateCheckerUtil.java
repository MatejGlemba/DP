package io.dpm.dropmenote.ws.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HateCheckerUtil {
    // Define a list of popular swear words and their variations
    private static final String[] SWEAR_WORDS = {
            "fuck",
            "fker",
            "fck",
            "f**k",
            "shit",
            "sht",
            "sit",
            "sh!t",
            "asshole",
            "a**hole",
            "ahole",
            "a-hole",
            "as$hole",
            "ahole",
            "bitch",
            "b*tch",
            "b!tch",
            "dick",
            "d*ck",
            "d!ck",
            "motherfucker",
            "motherfker",
            "motherf**ker",
            "pussy",
            "pu**y",
            "pus$y",
            "pu$sy",
            "puy",
            "cock",
            "cOck",
            "cck",
            "c0ck",
            "ass",
            "as$",
            "a$s",
            "a$$",
            "as",
            "as",
            "a**",
            "cunt",
            "cnt",
            "ct",
            "bastard",
            "twat",
            "wanker",
            "prick",
            "prck",
            "pr!ck",
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
            "deb!l",
            "kretén",
            "kurva",
            "krva",
            "krvenec",
            "k**venec",
            "suka",
            "ska"
    };
    public static boolean checkHate(final String text) {
        // Compile a regular expression pattern with the swear words and their variations
        String patternString = "\\b(" + String.join("|", SWEAR_WORDS) + ")(s|ed|ing|er)?\\b";
        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

        // Search for the pattern in the input text
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }
}