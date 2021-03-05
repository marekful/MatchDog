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
}
