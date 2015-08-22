package ua.lg.dev.utils.files;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FileSystem {
    private static final String PATH = "/Users/xDrake/cloud/google/photos/Италия/video/";
    //private static final String PATH = "/Users/xDrake/develop/test/";
    public static void main(String[] args) throws IOException {
        Path path = Paths.get(PATH);
        Map<String, Integer> map = new HashMap<>();

        Stream<Path> stream = Files.walk(path, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS);
        stream.filter(p -> {
            try {
                return Files.readAttributes(p, BasicFileAttributes.class).isRegularFile() && p.toString().endsWith("mp4");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }).forEach(p -> {
            try {

                BasicFileAttributes basicFileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
                LocalDateTime dateTime = LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(), ZoneId.systemDefault());

                MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");
                String checksum = getFileChecksum(shaDigest, p);

                String name = dateTime
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .replace("T", "_")
                        .replace(":", "-") + "_" + checksum.substring(0, 3) + ".mp4";

                System.out.println(p.getFileName().toString() + " -> " + name);
                Files.move(p, p.resolveSibling(name));

//                map.compute(name, (k, v) -> {
//                    if (v == null){
//                        v = 0;
//                    }else{
//                        //System.out.println(dateTime.toString() + " -> " + name);
//                    }
//                    v++;
//                    return v;
//                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        for (String s : map.keySet()) {
            if (map.get(s) > 1){
                //System.out.println(s + " " + map.get(s));
            }
        }
    }

    private static String getFileChecksum(MessageDigest digest, Path file) throws IOException
    {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file.toString());

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }
}
