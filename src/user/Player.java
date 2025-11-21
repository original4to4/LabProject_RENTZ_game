package user;

public class Player extends User{

    private int Player_ID;
    private int score;

    //Constructor
    public Player(String name, String email, String password) {
        super(name, email, password);
        score=0;
    }

    //Modifires

    public void addScore(int number){
        score+=number;
    }

    //Geter
    public int getScore(){
        return score;
    }

    //
    public int getPlayer_ID(){
        return Player_ID;
    }
}
