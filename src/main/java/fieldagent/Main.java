package fieldagent;

public class Main {
    public enum OS
    {
        linux, mac, windows;

       static public boolean isWindows() {
            return Main.os == windows;
        }
    }

    public static final OS os = System.getProperty("os.name").contains("Mac") ? OS.mac : (System.getProperty("os.name").toLowerCase().contains("win") ?  OS.windows : OS.linux);

    public static final String app = System.getProperty("appDir")+"/";
}
