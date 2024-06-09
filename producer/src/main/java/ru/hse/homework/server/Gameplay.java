package ru.hse.homework.server;

import ru.hse.homework.words.WordsReader;

import java.util.*;

public class Gameplay {
    private String hiddenWord = "";

    private int n = 5;

    private String wordsBase = "";

    private final ArrayList<ArrayList<Boolean>> guess = new ArrayList<>();

    private final ArrayList<Integer> scores = new ArrayList<>();

    private final Deque<Integer> queue = new ArrayDeque<>();

    private boolean isFinished = false;

    public void setN(int n) {
        this.n = n;
        this.hiddenWord = "";
    }

    public int getN() { return this.n; }

    public void setHiddenWord(String word) {
        this.hiddenWord = word;
        this.n = word.length();
    }

    public void setWordBase(String path) { this.wordsBase = path; }

    public synchronized void addPlayer() {
        guess.add(new ArrayList<>());
        scores.add(0);
    }

    public synchronized void erasePlayer(int i) {
        guess.remove(i);
        scores.remove(i);

        if (!queue.isEmpty() && queue.getFirst() > i) {
            queue.addFirst(queue.removeLast());
        }
    }

    public synchronized void start() {
        if (hiddenWord.isEmpty()) {
            Random gen = new Random();
            String[] words;

            if (wordsBase.isEmpty()) {
                words = WordsReader.readDefaultWords(n);
            } else {
                words = WordsReader.readWordsFromFile(wordsBase, n);
            }

            if (words.length == 0) {
                throw new RuntimeException("no words with current length found");
            }
            hiddenWord = words[gen.nextInt(words.length)];
        }

        n = hiddenWord.length();
        System.out.println(hiddenWord);

        for (int i = 0; i < guess.size(); i++) {
            queue.add(i);

            ArrayList<Boolean> cur = guess.get(i);
            for (int j = 0; j < n; j++) {
                cur.add(false);
            }
        }
    }

    public synchronized int getNextPlayerStep() {
        if (isFinished()) {
            return -1;
        }

        int ord = queue.removeFirst();
        while (ord >= guess.size()) {
            ord = queue.removeFirst();
        }
        queue.addLast(ord);
        return ord;
    }

    public synchronized int doAttempt(String letter, int place, int player) {
        if (isFinished) {
            return -1;
        }

        if (hiddenWord.contains(letter) && !guess.get(player).get(place)) {
            if (hiddenWord.charAt(place) == letter.charAt(0)) {
                guess.get(player).set(place, true);
                scores.set(player, scores.get(player) + 1);

                if (scores.get(player) == n) {
                    isFinished = true;
                }
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

        for (int i = 0; i < guess.size(); i++) {
            if (names.size() <= i) {
                break;
            }

            StringBuilder score = new StringBuilder();
            guess.get(i).stream().map(f -> { if (f) return '+'; else return '*';}).forEach(score::append);
            result.append(names.get(i)).append(String.format(" - %s\n", score));
        }

        return result.toString().split("\n");
    }

    public synchronized int getWinner() {
        if (isFinished()) {
            return queue.getLast();
        }
        return -1;
    }

    public synchronized String getHiddenWord() {
        return hiddenWord;
    }

    public synchronized boolean isFinished() {
        return isFinished;
    }
}
