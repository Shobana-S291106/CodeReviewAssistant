public class SafeCode {

    public static void main(String[] args) {

        String name = "Shobana";

        if ("Shobana".equals(name)) {
            System.out.println("User verified");
        }

        int total = 100;
        int count = 5;

        if (count != 0) {
            int result = total / count;
            System.out.println(result);
        }

        int[] numbers = {10, 20, 30, 40, 50};
        int index = 2;

        if (index >= 0 && index < numbers.length) {
            System.out.println(numbers[index]);
        }

        try {
            String value = "Hello";
            System.out.println(value);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
        }
    }
}