package ua.lg.dev.utils.files;


import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class DuplicateFinder {
    private AtomicLong counter = new AtomicLong(0);
    private Path sourcePath;
    private Path outputPath;
    private Map<String, List<Path>> hashes;

    private DuplicateFinder(String sourceDir, String outputDir) throws Exception {
        this.sourcePath = Paths.get(sourceDir);
        this.outputPath = Paths.get(outputDir);
        this.hashes = new ConcurrentHashMap<>();
    }

    public static void find(String sourceDir, String outputDir) throws Exception {
        DuplicateFinder finder = new DuplicateFinder(sourceDir, outputDir);
        long start = System.currentTimeMillis();
        finder.run();
        long finish = System.currentTimeMillis();
        System.out.println((finish - start) + "ms");
    }

    private void run() throws Exception {
        long totalSize = Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS).count();

        Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS)
                .parallel()
                .filter(path -> {
                    try {
                        return Files.readAttributes(path, BasicFileAttributes.class).isRegularFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .filter(path -> {
                    String name = path.getFileName().toString();
                    return !(name.equalsIgnoreCase("Icon.") || name.equalsIgnoreCase(".DS_Store"));
                })
                .forEach(path -> {
                    try {
                        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                        String checksum = getFileChecksum(messageDigest, path);

                        hashes.compute(checksum, (key, val) -> {
                            if (val == null) {
                                val = new CopyOnWriteArrayList<>();
                            }
                            val.add(path);
                            return val;
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    BigDecimal val = new BigDecimal(counter.getAndIncrement() * 100);
                    BigDecimal percent = val.divide(new BigDecimal(totalSize), MathContext.DECIMAL32);
                    System.out.println(percent.toString() + "%");
                });

        for (String key : hashes.keySet()) {
            List<Path> paths = hashes.get(key);

            if (paths.size() > 1) {
                System.out.println("Дубликаты:");
                paths.forEach(System.out::println);
                System.out.println("=================================");
            }
            //System.out.println(key);
            //hashes.get(key).stream().forEach(System.out::println);
            //System.out.println("=================================");

            Path pathFrom = paths.iterator().next();
            Path pathTarget = Paths.get(outputPath + pathFrom.toString().replace(sourcePath.toString(), ""));

//            System.out.println(pathFrom);
//            System.out.println(pathTarget);
//            System.out.println(pathTarget.getParent());

            if (!Files.exists(pathTarget.getParent())) {
                Files.createDirectories(pathTarget.getParent());
            }
            Files.copy(pathFrom, pathTarget, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private static String getFileChecksum(MessageDigest digest, Path file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file.toString());

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        ;

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }
}