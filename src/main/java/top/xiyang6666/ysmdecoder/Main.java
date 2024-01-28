package top.xiyang6666.ysmdecoder;

import org.apache.commons.cli.*;
import org.fusesource.jansi.Ansi;
import top.xiyang6666.ysmdecoder.ysm.YSMFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

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
        if (cmd.hasOption("input") ^ cmd.hasOption("output")) {
            System.err.printf("[Error] 未输入%s%n", cmd.hasOption("input") ? "output" : "input");
            return;
        }
        if (cmd.hasOption("input") && cmd.hasOption("output")) {
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
            System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[Success] ").reset().a(String.format("开始解码 %s -> %s", inputFile, outputDir)));

            Map<String, byte[]> fileMap = YSMFile.loadYsmFile(inputFile);
            for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileData = entry.getValue();
                File filePath = new File(outputDir, fileName);
                if (!filePath.getParentFile().isDirectory() && !filePath.getParentFile().mkdirs()) {
                    System.out.println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("[Warning] ").reset().a(String.format("无法创建文件夹: %s", filePath.getParent())));
                }
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(fileData);
                    System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[Success] ").reset().a(String.format("已写入文件: %s", fileName)));
                } catch (IOException exception) {
                    System.out.println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("[Warning] ").reset().a(String.format("无法写入文件: %s", fileName)));
                }
            }
            System.out.println(Ansi.ansi().fg(Ansi.Color.GREEN).a("[Success] ").reset().a("done."));
            return;
        }
        System.out.println("[Info] 无操作，程序已退出");
    }


    public static String getVersion() {
        String appVersion = null;
        try {
            Properties properties = new Properties();
            properties.load(Main.class.getClassLoader().getResourceAsStream("appinfo.properties"));
            appVersion = properties.getProperty("app.version");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appVersion;
    }
}