package cards;
public class Card {
    private int cardNumber;
    private char cardSuit; // H-Inima Rosie  S-Inima Neagra D-Romb  C-Trefla
    public Card (int cardNumber,  char culoare){
        this.cardNumber=cardNumber;
        this.cardSuit=culoare;
    }

    public int getCardNumber(){
        return cardNumber;
    }

    public char getCardSuit(){
        return cardSuit;
    }

    public void afisareCarte() {
        // Print card value
        if (cardNumber == 14) {
            System.out.print("A");
        } else if (cardNumber == 13) {
            System.out.print("K");
        } else if (cardNumber == 12) {
            System.out.print("Q");
        } else if (cardNumber == 11) {
            System.out.print("J");
        } else {
            System.out.print(cardNumber);
        }

        System.out.print(":"); // Fixed missing semicolon

        // Print suit
        if (cardSuit == 'H') {
            System.out.print("InimaRosie");
        } else if (cardSuit == 'S') {
            System.out.print("InimaNeagra"); // Fixed typo
        } else if (cardSuit == 'D') {
            System.out.print("Romb");
        } else if (cardSuit == 'C') {
            System.out.print("Trefla"); // Fixed condition
        }
    }

    public String nameOfPNG(){

        String rCardNumber="";
        String rCardSuit="";

        switch(this.cardSuit) {
            case 'H': rCardSuit = "hearts"; break;
            case 'S': rCardSuit = "spades"; break;
            case 'D': rCardSuit = "diamonds"; break;
            case 'C': rCardSuit = "clubs"; break;
        }

        switch(cardNumber) {
            case 11: rCardNumber = "jack"; break;
            case 12: rCardNumber = "queen"; break;
            case 13: rCardNumber = "king"; break;
            case 14: rCardNumber = "ace"; break;
            default: rCardNumber =String.valueOf(cardNumber);
        }

        return rCardNumber + "_of_" + rCardSuit + ".png";

    }

    public String description(){

        String rCardNumber="";
        String rCardSuit="";

        switch(this.cardSuit) {
            case 'H': rCardSuit = "hearts"; break;
            case 'S': rCardSuit = "spades"; break;
            case 'D': rCardSuit = "diamonds"; break;
            case 'C': rCardSuit = "clubs"; break;
        }

        switch(cardNumber) {
            case 11: rCardNumber = "jack"; break;
            case 12: rCardNumber = "queen"; break;
            case 13: rCardNumber = "king"; break;
            case 14: rCardNumber = "ace"; break;
            default: rCardNumber =String.valueOf(cardNumber);
        }

        return rCardNumber + "_of_" + rCardSuit + getSuitSymbol();

    }


    public String getSuitSymbol() {
        switch(cardSuit) {
            case 'H': return "♥";
            case 'S': return "♠";
            case 'D': return "♦";
            case 'C': return "♣";
            default: return "?";
        }
    }
}
// 11 - J
// 12 - Q
// 13 - K
// 14 - A