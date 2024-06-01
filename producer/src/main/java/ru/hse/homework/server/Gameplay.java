package ru.hse.homework.server;

import ru.hse.homework.words.WordsReader;
import java.util.*;

public class Gameplay {
    private String hiddenWord = "";

    private final ArrayList<Integer> scores = new ArrayList<>();

    private final Deque<Integer> queue = new ArrayDeque<>();

    private int n = 5;

    public void setN(int n) {
        this.n = n;
    }

    public int getN() { return this.n; }

    public void setHiddenWord(String word) {
        this.hiddenWord = word;
        this.n = word.length();
    }

    public synchronized void addPlayer() {
        scores.add(0);
    }

    public synchronized void erasePlayer(int i) {
        scores.remove(i);

        if (queue.getFirst() > i) {
            queue.addFirst(queue.removeLast());
        }
    }

    public synchronized void start() {
        if (hiddenWord.isEmpty()) {
            Random gen = new Random();
            String[] wordsBase = WordsReader.readWords(n);
            if (wordsBase.length == 0) {
                throw new RuntimeException("no words with current length found");
            }
            hiddenWord = wordsBase[gen.nextInt(wordsBase.length)];
        }

        System.out.println(hiddenWord);
        for (int i = 0; i < scores.size(); i++) {
            queue.add(i);
        }
    }

    public synchronized int getNextPlayerStep() {
        if (isFinished()) {
            return -1;
        }

        int ord = queue.removeFirst();
        while (ord >= scores.size()) {
            ord = queue.removeFirst();
        }
        queue.addLast(ord);
        return ord;
    }

    public synchronized int doAttempt(String letter, int place, int player) {
        if (isFinished()) {
            return -1;
        }

        if (hiddenWord.contains(letter)) {
            if (hiddenWord.charAt(place) == letter.charAt(0)) {
                hiddenWord = hiddenWord.substring(0, place) + '*' + hiddenWord.substring(place + 1);
                scores.add(player, scores.get(player) + 1);
                return 1;
            } else {
                return 0;
            }
        } else {
            return -1;
        }
    }

    public synchronized String[] getProgress(List<String> names) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < scores.size(); i++) {
            if (names.size() <= i) {
                break;
            }

            result.append(names.get(i)).append(String.format(" - %d\n", scores.get(i)));
        }

        return result.toString().split("\n");
    }

    public synchronized int getWinner() {
        if (isFinished()) {
            return queue.getLast();
        }
        return -1;
    }

    public synchronized boolean isFinished() {
        return hiddenWord.equals("*".repeat(hiddenWord.length()));
    }
}
