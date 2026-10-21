import java.util.Random;
public class FishManager{
        private Fish[] fishes = {
            new Fish("Clownfish", 5, 30, 5,new String[]{
                "fish/image1.png",
                "fish/image2.png",
                "fish/image3.png",
                "fish/image4.png"
            }),
            new Fish("Mackerel", 10, 22, 10,new String[]{
                "fish/Mackerel1.png",
                "fish/Mackerel2.png",
                "fish/Mackerel3.png",
                "fish/Mackerel4.png",
                "fish/Mackerel5.png"
            }),
            new Fish("Sea Bream", 15, 18, 15,new String[]{
                "fish/SeaBream1.png",
                "fish/SeaBream2.png",
                "fish/SeaBream3.png",
                "fish/SeaBream4.png",
                "fish/SeaBream5.png"
            }),
            new Fish("Grouper", 20, 12, 20,new String[]{
                "fish/Grouper1.png",
                "fish/Grouper2.png",
                "fish/Grouper3.png",
                "fish/Grouper4.png",
                "fish/Grouper5.png"
            }),
            new Fish("Barracuda", 30, 8, 25,new String[]{
                "fish/Barracuda1.png",
                "fish/Barracuda2.png",
                "fish/Barracuda3.png",
                "fish/Barracuda4.png",
                "fish/Barracuda5.png",
                "fish/Barracuda6.png"
            }),
            new Fish("Swordfish", 40, 5, 30,new String[]{
                "fish/Swordfish1.png",
                "fish/Swordfish2.png",
                "fish/Swordfish3.png",
                "fish/Swordfish4.png",
                "fish/Swordfish5.png",
                "fish/Swordfish6.png",
                "fish/Swordfish7.png"
            }),
            new Fish("Bluefin Tuna", 70, 4, 40,new String[]{
                "fish/BluefinTuna1.png",
                "fish/BluefinTuna2.png",
                "fish/BluefinTuna3.png",
                "fish/BluefinTuna4.png"
            }),
            new Fish("Mahi-Mahi", 150, 1, 50,new String[]{
                "fish/MahiMahi1.png",
                "fish/MahiMahi2.png",
                "fish/MahiMahi3.png",
                "fish/MahiMahi4.png"
            })
    };
    
    private Random random = new Random();

    public Fish catchFish() {
        int totalWeight = 0;

        for (Fish fish : fishes) {
            totalWeight += fish.getWeight();
        }
        int value = random.nextInt(totalWeight);

        for (Fish fish : fishes) {
            value -= fish.getWeight();

            if (value < 0){
                return fish;
            }
        }

        return fishes[0] ;
    }
}
