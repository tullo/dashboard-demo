package com.vaadin.demo.dashboard.data;

import java.util.Random;

public class Generator {

    private static final Random rand = new Random(1L);

    public static void reseed() {
        rand.setSeed(1L);
    }

    public static String randomFirstName() {
        String[] names = {"Dave", "Mike", "Katherine", "Jonas", "Linus",
            "Bob", "Anne", "Minna", "Elisa", "George", "Mathias", "Pekka",
            "Fredrik", "Kate", "Teppo", "Kim", "Samatha", "Sam", "Linda",
            "Jo", "Sarah", "Ray", "Michael", "Steve"};
        return names[rand.nextInt(names.length)];
    }

    public static String randomLastName() {
        String[] names = {"Smith", "Lehtinen", "Chandler", "Hewlett",
            "Packard", "Jobs", "Buffet", "Reagan", "Carthy", "Wu",
            "Johnson", "Williams", "Jones", "Brown", "Davis", "Moore",
            "Wilson", "Taylor", "Anderson", "Jackson", "White", "Harris",
            "Martin", "King", "Lee", "Walker", "Wright", "Clark",
            "Robinson", "Garcia", "Thomas", "Hall", "Lopez", "Scott",
            "Adams", "Barker", "Morris", "Cook", "Rogers", "Rivera",
            "Gray", "Price", "Perry", "Powell", "Russell", "Diaz"};
        return names[rand.nextInt(names.length)];
    }

    public static String randomCompanyName() {

        String name = randomName();

        final double rndDouble = rand.nextDouble();
        if (rndDouble < 0.03) {
            name += " Technologies";
        } else if (rndDouble < 0.02) {
            name += " Investment";
        }

        if (rndDouble < 0.3) {
            name += " Inc";
        } else if (rndDouble < 0.2) {
            name += " Ltd.";
        }

        return name;
    }

    public static String randomWord(int len, boolean capitalized) {
        String[] part = {"ger", "ma", "isa", "app", "le", "ni", "ke", "mic",
            "ro", "soft", "wa", "re", "lo", "gi", "is", "acc", "el", "tes",
            "la", "ko", "ni", "ka", "so", "ny", "mi", "nol", "ta", "pa",
            "na", "so", "nic", "sa", "les", "for", "ce"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            String p = part[rand.nextInt(part.length)];
            if (i == 0 && capitalized) {
                p = Character.toUpperCase(p.charAt(0)) + p.substring(1);
            }
            sb.append(p);
        }
        return sb.toString();

    }

    public static String randomText(int words) {
        StringBuilder sb = new StringBuilder();
        int sentenceWordsLeft = 0;
        while (words-- > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (sentenceWordsLeft == 0 && words > 0) {
                sentenceWordsLeft = rand.nextInt(15);
                sb.append(randomWord(1 + rand.nextInt(3), true));
            } else {
                sentenceWordsLeft--;
                sb.append(randomWord(1 + rand.nextInt(3), false));
                if (words > 0 && sentenceWordsLeft > 2 && rand.nextDouble() < 0.2) {
                    sb.append(',');
                } else if (sentenceWordsLeft == 0 || words == 0) {
                    sb.append('.');
                }
            }
        }
        return sb.toString();
    }

    public static String randomName() {
        int len = rand.nextInt(4) + 1;
        return randomWord(len, true);
    }

    public static String randomTitle(int words) {
        StringBuilder sb = new StringBuilder();
        int len = rand.nextInt(4) + 1;
        sb.append(randomWord(len, true));
        while (--words > 0) {
            len = rand.nextInt(4) + 1;
            sb.append(' ');
            sb.append(randomWord(len, false));
        }
        return sb.toString();
    }

    public static String randomHTML(int words) {
        StringBuilder sb = new StringBuilder();
        while (words > 0) {
            sb.append("<h2>");
            int len = rand.nextInt(4) + 1;
            sb.append(randomTitle(len));
            sb.append("</h2>");
            words -= len;
            int paragraphs = 1 + rand.nextInt(3);
            while (paragraphs-- > 0 && words > 0) {
                sb.append("<p>");
                len = rand.nextInt(40) + 3;
                sb.append(randomText(len));
                sb.append("</p>");
                words -= len;
            }
        }
        return sb.toString();
    }
}