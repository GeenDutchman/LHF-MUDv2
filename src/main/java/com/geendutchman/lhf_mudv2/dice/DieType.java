package com.geendutchman.lhf_mudv2.dice;

/**
 * Type of dice, like a d100, d20, d6, etc
 */
public enum DieType {
    HUNDRED(100), TWENTY(20), TWELVE(12), TEN(10), EIGHT(8), SIX(6), FOUR(4), TWO(2), ONE(1);

    /**
     * Transform a string into a type of die
     * 
     * @param value
     * @return
     */
    public static DieType getDieType(String value) {
        for (DieType dType : values()) {
            if (dType.toString().equalsIgnoreCase(value)) {
                return dType;
            }
        }
        return null;
    }

    /**
     * Is the string a type of die?
     * 
     * @param value
     * @return
     */
    public static boolean isDieType(String value) {
        return DieType.getDieType(value) != null;
    }

    private byte type;

    /**
     * Private constructor, whoohoo
     */
    private DieType(int type) {
        this.type = (byte) type;
    }

    /**
     * Protected constructor, whohoo
     * 
     * @param type
     */
    DieType(byte type) {
        this.type = type;
    }

    /**
     * Get the type of die as a byte
     * 
     * @return
     */
    public byte getType() {
        return this.type;
    }

    public String toString() {
        if (this.type <= 0) {
            return "";
        }
        return "" + this.type;
    }

}
