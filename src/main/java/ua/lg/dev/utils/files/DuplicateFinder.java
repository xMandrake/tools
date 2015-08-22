package ua.lg.dev.utils.files;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DuplicateFinder {
    private Path sourcePath;
    private Path outputPath;
    private Map<String, List<Path>> registry;

    private DuplicateFinder(String sourceDir, String outputDir) throws Exception {
        this.sourcePath = Paths.get(sourceDir);
        this.outputPath = Paths.get(outputDir);
        this.registry = new ConcurrentHashMap<>();
    }

    public static void find(String sourceDir, String outputDir) throws Exception {
        DuplicateFinder finder = new DuplicateFinder(sourceDir, outputDir);
        long start = System.currentTimeMillis();
        finder.run();
        long finish = System.currentTimeMillis();
        System.out.println(finish - start);
    }

    private void run() throws Exception {
        //90173
        Files.walk(sourcePath, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
                .parallel()
                .filter(path -> {
                    try {
                        return Files.readAttributes(path, BasicFileAttributes.class).isRegularFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .filter(path -> path.toString().endsWith("JPG") || path.toString().endsWith("jpg"))
//                .limit(100)
                .forEach(path -> {
                    try {
                        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                        String checksum = getFileChecksum(messageDigest, path);

                        registry.compute(checksum, (key, val) -> {
                            if (val == null) {
                                val = new CopyOnWriteArrayList<>();
                            }
                            val.add(path);
                            return val;
                        });

//                        System.out.println(path.getFileName() + " -> " + checksum);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        for (String key : registry.keySet()) {
            List<Path> paths = registry.get(key);
            if (paths.size() > 1){
                System.out.println(key + " -> " + paths.size());
                paths.forEach(path -> System.out.println(path.toString()));
                System.out.println("------------------------------------");
            }
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
