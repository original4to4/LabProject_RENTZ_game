package user;
import cards.*;
import java.util.ArrayList;
public class InGamePlayer {
    private Player player;
    private Hand hand;
    public ArrayList<Card> takenCards;

    //Constructor
    public InGamePlayer(Player player){//, Hand hand){
        this.player=player;
        this.hand = new Hand(); // Initialize hand
        this.takenCards = new ArrayList<>(); // Initialize takenCards
        //this.hand=hand;
    }

    //Getters
    public Player getPlayer(){
        return player;
    }

    public Hand getHand(){
        return hand;
    }
    //Setter
    public void setHand(Hand hand){
        this.hand=hand;
    }

    public void newHandTaken(ArrayList<Card> cards){
        for(Card card : cards){
            takenCards.add(card);
        }
    }

    public boolean removeCard(Card card){
        hand.removeCard(card);
        return true;
    }

    public ArrayList<Card> getTakenCards() {
        return takenCards;
    }
}
