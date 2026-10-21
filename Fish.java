import java.awt.Image;
import java.io.File;
import javax.swing.ImageIcon;

public class Fish {
    private String name;
    private int points;
    private int weight;
    private int speed;
    private Image[] images;

    public Fish(String name, int points,int weight,int speed,String[] imagePaths){
        this.name = name;
        this.points = points;
        this.weight = weight;
        this.speed = speed;
        this.images = new Image[imagePaths.length];

        for (int i = 0; i < imagePaths.length; i++) {
            String path = new File(System.getProperty("user.dir"), imagePaths[i]).getAbsolutePath();
            images[i] = new ImageIcon(path).getImage();
            
        }
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
    public Image getRandomImage() {
        if (images.length == 0) {
            return null;
            
        }
        return  images[(int)(Math.random() * images.length)];
    }

}
