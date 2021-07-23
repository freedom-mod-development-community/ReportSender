package xyz.fmdc.reportsender;

public class Hook {
    public final String developer, description, name, hookUrl, rsaPubKey;

    public Hook(String name, String developer, String description, String hookUrl, String rsaPubKey) {
        this.name = name;
        this.developer = developer;
        this.description = description;
        this.hookUrl = hookUrl;
        this.rsaPubKey = rsaPubKey;
    }
}
