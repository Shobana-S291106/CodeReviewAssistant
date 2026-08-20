public class NullPointerTest {

    public static void main(String[] args) {

        User user = getUser();

        System.out.println(user.getName());
    }

    private static User getUser() {
        return null;
    }
}

class User {

    public String getName() {
        return "Shobana";
    }
}