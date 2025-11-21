package user;
public class User {
    private String name;
    private String email;
    private String password;

    //Constructor
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password=password;
    }

    // Getters


    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword(){
        return password;
    }

    // Setters

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // toString method for easy debugging
    @Override
    public String toString() {
        return "Player{" +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password="+password+
                '}';
    }
}