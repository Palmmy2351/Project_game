public class Fish {
    private String name;
    private int points;
    private int weight;
    private int speed;

    public Fish(String name, int points,int weight,int speed){
        this.name = name;
        this.points = points;
        this.weight = weight;
        this.speed = speed;
    }
    public String getName(){
        return name;
    }
    public int getPoints(){
        return points;
    }
    public int getWeight(){
        return weight;
    }
    public int getSpeed(){
        return speed ;
    }
}
