import java.util.Random;
public class FishManager{
        private Fish[] fishes = {
        new Fish("Small Fish", 5, 30, 5),
        new Fish("Carp", 10, 22, 10),
        new Fish("Tilapia", 15, 18, 15),
        new Fish("Catfish", 20, 12, 20),
        new Fish("Salmon", 30, 8, 25),
        new Fish("Tuna", 40, 5, 30),
        new Fish("Golden Fish", 70, 4, 40),
        new Fish("Legendary Fish", 150, 1, 50)
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
