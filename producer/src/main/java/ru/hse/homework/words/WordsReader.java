package ru.hse.homework.words;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Utility to read text file containing one Russian noun per line into String array.
 * <p>
 * Since we have not discussed input yet, we provide easy to understand method
 * (based on knowledge available soo far). After discussing input/output we'll
 * be able to read it by half a dozen different ways.
 * <p>
 * For simplicity - txt-file with words must be located at the same package (directory) as the given source file.
 */
public class WordsReader {
    private WordsReader(){} // private constructor used to prevent instantiations

    /**
     * Resource fileName to read from.
     * Must be the neighbouring file to the WordsReader.java in the IDEA project.
     * The file can be replaced by some shorter file containing  word per line (for simplicity).
     */
    private static final String RESOURCE_FILENAME = "russian_nouns.txt";

    public static String getResourceFilename() {
        return RESOURCE_FILENAME;
    }
    /**
     * The method to read a resource file with words into string array.
     * @return - string array of words from the resource file.
     */
    public static String[] readDefaultWords(int n) {
        ArrayList<String> words = new ArrayList<>();
        InputStream inputStream = WordsReader.class.getClassLoader().getResourceAsStream(RESOURCE_FILENAME);
        if(inputStream != null) {
            try (Scanner scanner = new Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    String fileLine = scanner.nextLine();
                    String word = fileLine.trim();
                    if (word.length() == n) {
                        words.add(word);
                    }
                }
            }
        }
        return words.toArray(new String[0]);
    }

    public static String[] readWordsFromFile(String path, int n) {
        ArrayList<String> words = new ArrayList<>();
        InputStream inputStream;

        try {
            System.out.println(path);
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            return new String[0];
        }

        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                String fileLine = scanner.nextLine();
                String word = fileLine.trim();
                if (word.length() == n) {
                    words.add(word);
                }
            }
        }
        return words.toArray(new String[0]);
    }
}
