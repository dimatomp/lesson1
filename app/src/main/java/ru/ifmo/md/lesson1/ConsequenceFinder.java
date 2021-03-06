package ru.ifmo.md.lesson1;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by dimatomp on 13.09.14.
 */
public class ConsequenceFinder extends Thread {
    static final int P = 1000_000_007;
    static final int MAX_CAPACITY = 100;
    int[][][] history = new int[MAX_CAPACITY][][];
    private static final String TAG = "ConsequenceFinder";
    final FieldDrawer callbackInstance;
    HashMap<HashSetEntry, Integer> hashes = new HashMap<>();
    int historyIndex;

    public ConsequenceFinder(FieldDrawer callbackInstance) {
        this.callbackInstance = callbackInstance;
        start();
    }

    public synchronized void addToHistory(int[][] field) {
        if (historyIndex < MAX_CAPACITY && history != null) {
            history[historyIndex] = field;
            notify();
        }
        historyIndex++;
    }

    @Override
    public void run() {
        int curIndex = 0;
        while (curIndex < MAX_CAPACITY) {
            while (curIndex >= historyIndex)
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            Log.v(TAG, "Processing entry " + curIndex);
            HashSetEntry entry = new HashSetEntry(curIndex);
            if (!hashes.containsKey(entry))
                hashes.put(entry, curIndex);
            else {
                Log.d(TAG, "I have definitely seen this before... Maybe speed them up a little?");
                int i = hashes.get(entry);
                int history2[][][] = new int[curIndex - 1 - i][][];
                System.arraycopy(history, i, history2, 0, curIndex - 1 - i);
                history = null; // removing garbage
                Bitmap animation[] = new Bitmap[curIndex - 1 - i];
                FieldRenderer renderer = new FieldRenderer(history2[0].length, history2[0][0].length);
                for (int j = 0; j < history2.length; j++)
                    animation[j] = renderer.draw(history2[j]);
                synchronized (this) {
                    callbackInstance.replaceWithAnimation(animation, historyIndex - curIndex);
                }
                break;
            }
            curIndex++;
        }
        Log.d(TAG, "Stopped looking for loops.");
        hashes = null;
        history = null;
    }

    class HashSetEntry {
        final int number;
        Integer theHash = null;

        HashSetEntry(int number) {
            this.number = number;
        }

        @Override
        public int hashCode() {
            if (theHash == null) {
                theHash = 0;
                int[][] field = history[number];
                for (int i = 0; i < field.length; i++)
                    for (int j = 0; j < field[i].length; j++)
                        theHash = (theHash * P) + field[i][j];
            }
            return theHash;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof HashSetEntry) {
                int[][] mine = history[number];
                int[][] his = history[((HashSetEntry) o).number];
                for (int i = 0; i < mine.length; i++)
                    if (!Arrays.equals(mine[i], his[i]))
                        return false;
                return true;
            }
            return false;
        }
    }
}
