package ru.hse.homework.server;

import ru.hse.homework.words.WordsReader;

import java.util.*;

/**
 * Gameplay is class that implementing game logic as determined by the rules of the game.
 */
public class Gameplay {
    /**
     * Hidden word. Empty if word don't guessed yet.
     */
    private String hiddenWord = "";

    /**
     * Length of hidden word. By default - 5.
     */
    private int n = 5;

    /**
     * File path to wod base. By default - russian_nouns.txt.
     */
    private String wordsBase = "russian_nouns.txt";

    /**
     * Array with success of players. Each player represent as array of boolean that indicates guessed current player
     * current letter or not.
     */
    private final ArrayList<ArrayList<Boolean>> guess = new ArrayList<>();

    /**
     * Array with total count of guessed letters for each player.
     */
    private final ArrayList<Integer> scores = new ArrayList<>();

    /**
     * Queue with order of attempt.
     */
    private final Deque<Integer> queue = new ArrayDeque<>();

    /**
     * Flag that indicates that game is finished.
     */
    private boolean isFinished = false;

    /**
     * Setter for length of hidden word.
     * @param n - length of word.
     */
    public void setN(int n) {
        this.n = n;
        this.hiddenWord = "";
    }

    /**
     * Getter for length of hidden word.
     * @return length of word.
     */
    public int getN() { return this.n; }

    /**
     * Setter for current hidden word.
     * @param word - current hidden word.
     */
    public void setHiddenWord(String word) {
        this.hiddenWord = word;
        this.n = word.length();
    }

    /**
     * Setter for file path to word base.
     * @param path - file path.
     */
    public void setWordBase(String path) { this.wordsBase = path; }

    /**
     * Add new player.
     */
    public synchronized void addPlayer() {
        guess.add(new ArrayList<>());
        scores.add(0);
    }

    /**
     * Erase some player.
     * @param i - index of players to erase.
     */
    public synchronized void erasePlayer(int i) {
        guess.remove(i);
        scores.remove(i);

        if (!queue.isEmpty() && queue.getFirst() > i) {
            queue.addFirst(queue.removeLast());
        }
    }

    /**
     * Start game. Guess the hidden word. If there is no words on word base and hidden word doesn't set explicitly
     * then throw Runtime Exception. Fill the queue and guess array.
     */
    public synchronized void start() {
        if (hiddenWord.isEmpty()) {
            Random gen = new Random();
            String[] words;

            if (wordsBase.isEmpty() || wordsBase.equals("russian_nouns.txt")) {
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

    /**
     * Getter for index of next player that should take attempt.
     * @return index of next player that should take attempt.
     */
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

    /**
     * Do attempt. Check the result of attempt and update score and guess array. If all letters guessed, then game
     * is finished.
     * @param letter - guessed letter
     * @param place - guessed place
     * @param player - index of player
     * @return result of attempt. 1 - if this letter is in word on current place,
     *          0 - if letter exist in word, but on other place.
     *          -1 - if there is no this letter in word.
     */
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

    /**
     * Getter for game progress.
     * @param names - usernames of players.
     * @return - array strings like "player - ***+*+" where '*' - means player not guessed this letter and
     *      '+' - otherwise.
     */
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

    /**
     * Getter for index of player, that win the game.
     * @return index of player, that win the game.
     */
    public synchronized int getWinner() {
        if (isFinished()) {
            return queue.getLast();
        }
        return -1;
    }

    /**
     * Getter for hidden word.
     * @return hidden word.
     */
    public synchronized String getHiddenWord() {
        return hiddenWord;
    }

    /**
     * Getter for flag isFinished.
     * @return true - if game finished and false - otherwise.
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }
}
