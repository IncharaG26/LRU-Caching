import java.io.*;
import java.util.*;

class CacheEntry {
    String fileId;
    int lastAccessTime;
    int cost; // File size in KB

    public CacheEntry(String fileId, int time, int cost) {
        this.fileId = fileId;
        this.lastAccessTime = time;
        this.cost = cost;
    }
}

public class RealTimeOCRCache {
    private final int cacheSize;
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();
    private int currentTime = 0;
    private int hits = 0, misses = 0, totalCost = 0;
    private final File fileDir = new File("Files");


    public RealTimeOCRCache(int size) {
        this.cacheSize = size;
        if (!fileDir.exists()) fileDir.mkdir();
    }

    public void createFile(String fileId, int sizeKB) throws IOException {
        File file = new File(fileDir, fileId);
        if (file.exists()) {
            System.out.println("File already exists.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(sizeKB * 1024L);
        }
        System.out.println("Created file: " + fileId + " (" + sizeKB + " KB)");
    }

    public void accessFile(String fileId) throws IOException, InterruptedException {
        currentTime++;
        File file = new File(fileDir, fileId);

        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        int cost = (int) (file.length() / 1024);
        System.out.println("Accessing: " + fileId + " (Cost: " + cost + ")");

        if (cache.containsKey(fileId)) {
            cache.get(fileId).lastAccessTime = currentTime;
            hits++;
            System.out.println("HIT\n");
        } else {
            System.out.println("MISS");
            misses++;
            totalCost += cost;

            if (cache.size() >= cacheSize) {
                evictLowestCostFile();
            }

            cache.put(fileId, new CacheEntry(fileId, currentTime, cost));
            System.out.println("Cache: " + cache.keySet() + "\n");

        }
    }

   private void evictLowestCostFile() {
    String toEvict = null;
    int minAccessTime = Integer.MAX_VALUE;
    int minCost = Integer.MAX_VALUE;

    for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
        CacheEntry ce = entry.getValue();
        if (ce.lastAccessTime < minAccessTime) {
            minAccessTime = ce.lastAccessTime;
            minCost = ce.cost;
            toEvict = ce.fileId;
        } else if (ce.lastAccessTime == minAccessTime && ce.cost < minCost) {
            minCost = ce.cost;
            toEvict = ce.fileId;
        }
    }

    if (toEvict != null) {
        CacheEntry removed = cache.remove(toEvict);
        System.out.println("Evicted: " + toEvict + " (Cost: " + removed.cost + ", LastAccess: " + removed.lastAccessTime + ")");
    }
}


    public void printStats() {
        System.out.println("\n--- Cache Statistics ---");
        System.out.println("Total Requests: " + (hits + misses));
        System.out.println("Hits: " + hits);
        System.out.println("Misses: " + misses);
        System.out.println("Total Cost: " + totalCost);
        System.out.printf("Hit Ratio: %.2f%%\n", 100.0 * hits / (hits + misses));
    }

    public void listFiles() {
        File[] files = fileDir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files available.");
            return;
        }
        System.out.println("Available Files:");
        for (File f : files) {
            System.out.println("  - " + f.getName() + " (" + f.length() / 1024 + " KB)");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter cache size: ");
        int size = sc.nextInt();
        RealTimeOCRCache cache = new RealTimeOCRCache(size);

        while (true) {
            System.out.println("\nMenu:\n1. Create File\n2. Access File\n3. List Files\n4. View Cache Stats\n5. Exit");
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter file name: ");
                    String fname = sc.next();
                    System.out.print("Enter file size (KB): ");
                    int fsize = sc.nextInt();
                    cache.createFile(fname, fsize);
                }
                case 2 -> {
                    System.out.print("Enter file name to access: ");
                    String afile = sc.next();
                    cache.accessFile(afile);
                }
                case 3 -> cache.listFiles();
                case 4 -> cache.printStats();
                case 5 -> {
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}
