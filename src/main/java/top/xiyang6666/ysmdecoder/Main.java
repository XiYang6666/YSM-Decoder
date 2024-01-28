package top.xiyang6666.ysmdecoder;

import org.apache.commons.cli.*;
import org.fusesource.jansi.Ansi;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import top.xiyang6666.ysmdecoder.ysm.YSMFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        //@formatter:off
        Options options = new Options()
                .addOption(new Option("v", "version", false, "输出版本"))
                .addOption(new Option("i", "input", true, "输入文件"))
                .addOption(new Option("o", "output", true, "输出文件"));
        //@formatter:on

        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("参数错误,无法解析参数.");
            return;
        }

        if (cmd.hasOption("version")) {
            System.out.printf("YSM-Decoder v%s.%n", getVersion());
            return;
        }
        if (cmd.hasOption("input") && cmd.hasOption("output")) {
            System.out.println(cmd.getOptionValue("input"));
            System.out.println(cmd.getOptionValue("output"));

            File inputFile = new File(cmd.getOptionValue("input"));
            File outputDir = new File(cmd.getOptionValue("output"));

            if (!inputFile.isFile()) {
                System.err.println("[Error] 输入文件无效");
                return;
            }
            if (!outputDir.isDirectory()) {
                System.err.println("[Error] 输出目录无效");
                return;
            }

            Map<String, byte[]> fileMap = YSMFile.loadYsmFile(inputFile);
            for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileData = entry.getValue();
                File filePath = new File(outputDir, fileName);
                if (!filePath.getParentFile().mkdirs()) {
                    System.out.println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("[Warning] ").reset().a(String.format("无法创建文件夹: %s", filePath.getParent())));
                }
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(fileData);
                    System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[Success] ").reset().a(String.format("已写入文件: %s", fileName)));
                } catch (IOException exception) {
                    System.out.println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("[Warning] ").reset().a(String.format("无法写入文件: %s", fileName)));
                }
            }
            System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[Success]").reset().a("done."));
        }
    }


    public static String getVersion() {
        String version = null;
        try {
            File pomFile = new File("pom.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);
            doc.getDocumentElement().normalize();

            Node versionNode = doc.getElementsByTagName("version").item(0);
            version = versionNode.getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }
}