package matchdog.fibsboard;

public class Dice {
    private final int die1;
    private final int die2;

    Dice(int die1, int die2) {
        this.die1 = die1;
        this.die2 = die2;
    }

    public int getDie1() {
        return die1;
    }

    public int getDie2() {
        return die2;
    }

    public int getSmaller() {
        if (die1 == die2) {
            return die1;
        }
        return Math.min(die1, die2);
    }

    public int getGreater() {
        if (die1 == die2) {
            return die1;
        }
        return Math.max(die1, die2);
    }

    public boolean notDouble() {
        return die1 != die2;
    }

    public boolean isDouble() {
        return die1 == die2;
    }
}
