import java.util.Random;
public class FishManager{
        private Fish[] fishes = {
            new Fish("Clownfish", 50000, 30, 5,new String[]{
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
                "fish/Swordfish6.png"
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

    // Finds a fish by its name (the client uses this to show the fish the server chose)
    public Fish findByName(String name) {
        for (Fish fish : fishes) {
            if (fish.getName().equals(name)) {
                return fish;
            }
        }
        return null;
    }

    // Maps the speed of the red line to the speed of the fish that can be caught
    private int fishSpeedFor(int barSpeed) {
        if (barSpeed < 10) return 5;
        if (barSpeed < 15) return 10;
        if (barSpeed < 20) return 15;
        if (barSpeed < 25) return 20;
        if (barSpeed < 30) return 25;
        if (barSpeed < 40) return 30;
        if (barSpeed < 50) return 40;
        return 50;
    }

    // Picks a random fish of the matching speed (a higher weight means more likely)
    public Fish catchFish(int barSpeed) {
        int target = fishSpeedFor(barSpeed);

        int totalWeight = 0;
        for (Fish fish : fishes) {
            if (fish.getSpeed() == target) {
                totalWeight += fish.getWeight();
            }
        }

        if (totalWeight == 0) {
            return null;
        }

        int value = random.nextInt(totalWeight);
        for (Fish fish : fishes) {
            if (fish.getSpeed() == target) {
                value -= fish.getWeight();
                if (value < 0) {
                    return fish;
                }
            }
        }

        return null;
    }
}