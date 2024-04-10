package dev.melledy.importer;

import java.io.File;
import java.net.URLClassLoader;

import org.slf4j.Logger;

import emu.lunarcore.LunarCore;
import emu.lunarcore.command.Command;
import emu.lunarcore.plugin.Plugin;

public class ImporterPlugin extends Plugin {

    public ImporterPlugin(Identifier identifier, URLClassLoader classLoader, File dataFolder, Logger logger) {
        super(identifier, classLoader, dataFolder, logger);
    }

    public void onLoad() {
        
    }
    
    public void onEnable() {
        LunarCore.getCommandManager().registerCommand(new ImportCommand());
    }
    
    public void onDisable() {
        LunarCore.getCommandManager().unregisterCommand(ImportCommand.class.getAnnotation(Command.class).label());
    }

}
