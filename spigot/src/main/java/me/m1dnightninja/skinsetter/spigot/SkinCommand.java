package me.m1dnightninja.skinsetter.spigot;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.IPlayerDataModule;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.module.skin.Skin;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.spigot.module.lang.LangModule;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import me.m1dnightninja.skinsetter.api.SkinSetterAPI;
import me.m1dnightninja.skinsetter.common.SkinUtil;
import me.m1dnightninja.skinsetter.spigot.integration.CitizensIntegration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkinCommand implements CommandExecutor, TabCompleter {

    private final boolean CITIZENS_ENABLED;
    private final SkinUtil util;

    private final List<String> subcommands = Arrays.asList("set", "reset", "save", "reload", "setdefault", "cleardefault", "persistence");

    public SkinCommand(SkinUtil util) {
        this.util = util;
        this.CITIZENS_ENABLED = Bukkit.getPluginManager().isPluginEnabled("citizens");

        if(CITIZENS_ENABLED) {
            subcommands.add("setnpc");
        }

    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String s, @Nonnull String[] args) {

        List<String> suggestions = new ArrayList<>();

        switch (args.length) {
            case 0:
            case 1:
                for(String str : subcommands) {
                    if(sender.hasPermission("skinstter.command." + str)) suggestions.add(str);
                }
                break;
            case 2:
                if(args[0].equals("reload")) break;
                if(subcommands.contains(args[0]) && sender.hasPermission("skinsetter.command." + args[0])) {
                    if ((CITIZENS_ENABLED && args[0].equals("setnpc")) || args[0].equals("setdefault")) {

                        suggestions.addAll(util.getSkinNames());

                    } else if(args[0].equals("persistence")) {

                        suggestions.add("enable");
                        suggestions.add("disable");

                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            suggestions.add(p.getDisplayName());
                        }
                    }
                }
                break;
            case 3:
                if(args[0].equals("reload")) break;
                if(args[0].equals("set")) {
                    suggestions.addAll(util.getSkinNames());
                }
                break;
            case 4:
                if(args[0].equals("set")) suggestions.add("-o");
        }

        List<String> out = new ArrayList<>();
        for(String sug : suggestions) {
            if(sug.startsWith(args[args.length - 1])) {
                out.add(sug);
            }
        }

        return out;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String s, @Nonnull String[] args) {

        if(!sender.hasPermission("skinsetter.command")) {
            LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.no_permission");
            return true;
        }

        if(args.length == 0) {
            sendArgs(sender);
            return true;
        }

        if(!sender.hasPermission("skinsetter.command." + args[0])) {
            LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.no_permission");
            return true;
        }

        switch(args[0]) {
            case "set":

                if(args.length < 3) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin set <player> [id/name] (-o)"));
                    return true;
                }

                Player p = Bukkit.getPlayerExact(args[1]);
                String id = args[2];

                if(p == null) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.invalid_name");
                    return true;
                }

                MPlayer mp = SpigotPlayer.wrap(p);
                Skin skin = util.getSavedSkin(id);

                boolean original  = (args.length > 3 && args[3].equals("-o"));

                if(original || skin == null) {

                    Player other = Bukkit.getPlayerExact(id);
                    if(other == null || (original && !Bukkit.getServer().getOnlineMode())) {

                        LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.set.online", new CustomPlaceholderInline("name", id));

                        util.getSkinOnline(id, (uid, oskin) -> {
                            if (oskin == null) {
                                LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.invalid_name");
                                return;
                            }
                            util.setSkin(mp, oskin);
                            LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.set.result", mp);
                        });

                    } else {

                        MPlayer mo = SpigotPlayer.wrap(other);
                        skin = original ? util.getLoginSkin(mo) : util.getSkin(mo);

                        util.setSkin(mp, skin);
                        LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.set.result", mp);

                    }

                } else {

                    util.setSkin(mp, skin);
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.set.result", mp);
                }

                break;

            case "reset":

                if(args.length != 2) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin reset <player>"));
                    return true;
                }

                p = Bukkit.getPlayerExact(args[1]);

                if(p == null) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.invalid_name");
                    return true;
                }

                mp = SpigotPlayer.wrap(p);

                util.resetSkin(mp);
                LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.reset.result", mp);

                break;

            case "save":

                if(args.length != 3) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin save <player> [id]"));
                    return true;
                }

                p = Bukkit.getPlayerExact(args[1]);
                id = args[2];

                if(util.getSavedSkin(id) != null && !sender.hasPermission("skinsetter.overwrite_skins")) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.no_overwrite");
                    return true;
                }

                if(p == null) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.invalid_name");
                    return true;
                }

                mp = SpigotPlayer.wrap(p);

                util.saveSkin(mp, id);
                LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.save.result", mp, new CustomPlaceholderInline("id", id));
                break;

            case "setnpc":

                if(!CITIZENS_ENABLED) {
                    sendArgs(sender);
                    return true;
                }
                if(!(sender instanceof Player)) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.not_player");
                    return true;
                }
                if(args.length != 2) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin setnpc <skin>"));
                    return true;
                }

                Skin sn_s = util.getSavedSkin(args[1]);

                CitizensIntegration.setNPCSkin((Player) sender, sn_s).send(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(((Player) sender).getUniqueId()));
                break;

            case "reload":

                long time = System.currentTimeMillis();
                SkinSetterAPI.getInstance().reloadConfig();
                time = System.currentTimeMillis() - time;
                LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.reload.result", new CustomPlaceholderInline("time", time+""));

                break;

            case "setdefault":

                if(args.length != 2) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin setdefault <skin>"));
                    return true;
                }

                id = args[1];
                skin = SkinSetterAPI.getInstance().getSkinRegistry().getSkin(id);

                if(skin == null) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.invalid_skin");
                    return true;
                }

                SkinSetterAPI.getInstance().DEFAULT_SKIN = skin;
                SkinSetterAPI.getInstance().getConfig().set("default_skin", id);
                SkinSetterAPI.getInstance().saveConfig();

                LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.setdefault.result", new CustomPlaceholderInline("id", id));

                break;

            case "cleardefault":

                SkinSetterAPI.getInstance().DEFAULT_SKIN = null;
                SkinSetterAPI.getInstance().getConfig().set("default_skin", "");
                SkinSetterAPI.getInstance().saveConfig();

                LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.cleardefault.result");

                break;

            case "persistence":

                if(args.length != 2) {
                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin persistance <enable/disable>"));
                    return true;
                }

                String action = args[1];
                if(action.equals("enable")) {

                    SkinSetterAPI.getInstance().PERSISTENT_SKINS = true;
                    SkinSetterAPI.getInstance().getConfig().set("persistent_skins", true);
                    SkinSetterAPI.getInstance().saveConfig();

                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.persistence.result.enabled");

                } else if(action.equals("disable")) {

                    SkinSetterAPI.getInstance().PERSISTENT_SKINS = false;
                    SkinSetterAPI.getInstance().getConfig().set("persistent_skins", false);
                    SkinSetterAPI.getInstance().saveConfig();

                    IPlayerDataModule mod = MidnightCoreAPI.getInstance().getModule(IPlayerDataModule.class);

                    for(MPlayer pl : MidnightCoreAPI.getInstance().getPlayerManager()) {

                        mod.getPlayerData(pl.getUUID()).set("skinsetter", null);
                        mod.savePlayerData(pl.getUUID());
                    }

                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.persistence.result.disabled");

                } else {

                    LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/skin persistance <enable/disable>"));
                }

                break;
        }

        return true;
    }

    private void sendArgs(CommandSender sender) {

        StringBuilder builder = new StringBuilder(ChatColor.RED + "/skin <");
        int found = 0;
        for(String cmd : subcommands) {
            if(sender.hasPermission("skinsetter.command." + cmd)) {
                if(found > 0) {
                    builder.append("/");
                }
                builder.append(cmd);
                found++;
            }
        }
        builder.append(">");

        if(found == 0) {
            LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.no_permission");
        }

        LangModule.sendMessage(sender, SkinSetterAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", builder.toString()));

    }

}
