
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;


public class InlierTaskManager {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<String, UserProfile> users = new ConcurrentHashMap<>();
    private static final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    private static int taskCounter = 1;
    private static int userCounter = 1;

    static class UserProfile {
        String name;
        double totalEarnings = 0;
        LocalDateTime loginTime;
        Task currentTask;
        List<Task> completedTasks = new ArrayList<>();
        List<Task> uncompletedTasks = new ArrayList<>();

        UserProfile(String name) {
            this.name = name;
            this.loginTime = LocalDateTime.now();
        }
    }

    static class Task {
        int id;
        UserProfile assignedUser;
        LocalDateTime assignedTime;
        LocalDateTime completedTime;
        boolean isCompleted = false;

        long completionDuration; // Total seconds worked
        double taskEarnings; // Earnings for this specific task

        Task(UserProfile user) {
            this.id = taskCounter++;
            this.assignedUser = user;
            this.assignedTime = LocalDateTime.now();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        while (true) {
            displayMenu();


            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        registerUser(scanner);
                        break;
                    case 2:
                        assignTask(scanner);
                        break;
                    case 3:
                        submitTask(scanner);
                        break;
                    case 4:
                        calculateEarnings(scanner);
                        break;
                    case 5:
                        displayAdminPanel();
                        break;
                    case 6:
                        System.exit(0);
                    default:
                        System.out.println("İnvalid option!");
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Enter a number.");
                scanner.nextLine(); // Clear the invalid input
            }
        }

    }

    private static void displayMenu() {

        System.out.println("1. Register");
        System.out.println("2. Take Task");
        System.out.println("3. Submit Task");
        System.out.println("4. Earnings");
        System.out.println("5. Admin Panel");
        System.out.println("6. Exit");
        System.out.print("Select: ");
    }

    private static void registerUser(Scanner scanner) {
        while (true) {
            System.out.print("Enter your name: ");
            String name = scanner.nextLine().trim();

            // İsmin sadece harflerden oluşup oluşmadığını kontrol et
            if (!name.matches("^[a-zA-ZğüşöçİĞÜŞÖÇ]+$")) {
                System.out.println("Invalid name. Please use only letters.");
                continue;
            }

            if (users.values().stream().anyMatch(user -> user.name.equals(name))) {
                System.out.println("User already exists!");
                continue;
            }

            UserProfile newUser = new UserProfile(name);
            users.put(name, newUser);
            System.out.println("User registered successfully!");
            break;
        }
    }

    private static void assignTask(Scanner scanner) {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        UserProfile user = users.get(name);
        if (user == null) {
            System.out.println("User not found. Register first!");
            return;
        }

        if (user.currentTask != null) {
            System.out.println("You are already working on a task!");
            return;
        }

        Task newTask = new Task(user);
        user.currentTask = newTask;
        tasks.put(newTask.id, newTask);

        System.out.printf("Task assigned: Task ID %d assigned to %s at %s%n",
                newTask.id, name, newTask.assignedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        executor.submit(() -> {
            try {
                Thread.sleep(60000); // 1 minute task duration
                synchronized (user) {
                    if (!newTask.isCompleted) {
                        System.err.println("\n🔴 WARNING: Task " + newTask.id +
                                " for user " + name +
                                " has expired!");
                        user.uncompletedTasks.add(newTask);
                        user.currentTask = null;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static void submitTask(Scanner scanner) {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        UserProfile user = users.get(name);
        if (user == null) {
            System.out.println("User not found!");
            return;
        }

        if (user.currentTask == null) {
            System.out.println("No active task to submit!");
            return;
        }

        Task task = user.currentTask;
        task.completedTime = LocalDateTime.now();
        task.isCompleted = true;
        task.completionDuration = ChronoUnit.SECONDS.between(task.assignedTime, task.completedTime);
        task.taskEarnings = calculateTaskEarnings(task.completionDuration);

        user.completedTasks.add(task);
        user.currentTask = null;
        user.totalEarnings += task.taskEarnings;

        System.out.println("Task " + task.id + " submitted successfully!");
        System.out.println("Task duration: " + task.completionDuration + " seconds");
        System.out.println("Earnings for this task: $" + String.format("%.2f", task.taskEarnings));
    }

    private static void calculateEarnings(Scanner scanner) {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        UserProfile user = users.get(name);
        if (user == null) {
            System.out.println("User not found!");
            return;
        }

        System.out.println("\n--- Earnings Details for " + name + " ---");
        System.out.printf("Total Earnings: $%.2f%n", user.totalEarnings);
        System.out.println("\nCompleted Tasks:");
        System.out.println("Task ID\t\tTask Duration (s)\tTask Earnings");

        for (Task task : user.completedTasks) {
            System.out.printf("\t%d\t\t\t%d\t\t\t\t\t$%.2f%n\n",
                    task.id,
                    task.completionDuration,
                    task.taskEarnings
            );
        }
    }

    private static double calculateTaskEarnings(long seconds) {
        double earnings = 0;
        if (seconds <= 30) {
            earnings = (seconds / 30.0) * 10.0;
        } else {
            earnings = 10.0 + ((seconds - 30) / 30.0) * 5.0;
        }
        return earnings;
    }

    private static void displayAdminPanel() {
        System.out.println("\n--- Admin Panel ---");
        for (UserProfile user : users.values()) {
            System.out.println("User: " + user.name);
            System.out.println("Completed Tasks: " + user.completedTasks.size());
            System.out.println("Uncompleted Tasks: " + user.uncompletedTasks.size());
            System.out.println("Total Earnings: $" + String.format("%.2f", user.totalEarnings));
            System.out.println("-------------------");
        }
    }
}