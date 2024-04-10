package dev.melledy.importer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import dev.melledy.importer.enka.EnkaApiHelper;
import dev.melledy.importer.enka.EnkaUserData;
import emu.lunarcore.command.Command;
import emu.lunarcore.command.CommandArgs;
import emu.lunarcore.command.CommandHandler;
import emu.lunarcore.data.GameData;
import emu.lunarcore.game.avatar.GameAvatar;
import emu.lunarcore.game.inventory.GameItem;
import emu.lunarcore.game.inventory.GameItemSubAffix;
import emu.lunarcore.game.inventory.tabs.InventoryTabType;
import emu.lunarcore.server.packet.send.PacketPlayerSyncScNotify;
import emu.lunarcore.util.Utils;

@Command(label = "import", permission = "player.import", requireTarget = true, desc = "/import [official server uid]")
public class ImportCommand implements CommandHandler {
    
    @Override
    public void execute(CommandArgs args) {
        // Parse uid
        long uid = Utils.parseSafeLong(args.get(0));
        if (uid == 0) {
            args.sendMessage("Error: Invalid uid");
            return;
        }
        
        // Sanity check materials
        var inventory = args.getTarget().getInventory();
        if (inventory.getTab(InventoryTabType.RELIC).getAvailableCapacity() <= 0 || inventory.getTab(InventoryTabType.EQUIPMENT).getAvailableCapacity() <= 0) {
            args.sendMessage("Error: The targeted player does not has enough space in their relic/lightcone inventory");
            return;
        }
        
        // Import from enka
        EnkaApiHelper.fetchAsync(uid).whenComplete((result, throwable) -> {
            if (throwable != null) {
                String error = throwable.getCause() != null ? throwable.getCause().getLocalizedMessage() : throwable.getLocalizedMessage();
                args.sendMessage("Enka error: " + error);
                return;
            }
            
            if (result != null) {
                int count = parseUserData(args, result);
                args.getSender().sendMessage(count + " avatars imported successfully from " + result.getDetailInfo().getNickname());
            }
        });
    }

    @SuppressWarnings("deprecation")
    private int parseUserData(CommandArgs args, EnkaUserData user) {
        int count = 0;
        var player = args.getTarget();
        var detail = user.getDetailInfo();
        
        // Copy avatars
        for (var avatarDetail : detail.getAvatarDetailList()) {
            GameAvatar avatar = player.getAvatarById(avatarDetail.getAvatarId());
            
            if (avatar == null) {
                // Validate avatar excel (In case we are using an older version of the server)
                var excel = GameData.getAvatarExcelMap().get(avatarDetail.getAvatarId());
                if (excel == null) continue;
                
                // Create avatar
                avatar = new GameAvatar(excel);
                
                // Gives the avatar to player
                player.addAvatar(avatar);
            }
            
            // Check if avatar is hero
            if (avatar.isHero()) {
                // TODO handle correctly later
                continue;
            }

            // Set avatar basic data
            avatar.setLevel(avatarDetail.getLevel());
            avatar.setExp(0);
            avatar.setRank(avatarDetail.getRank());
            avatar.setPromotion(avatarDetail.getPromotion());
            avatar.setRewards(0b00101010);
            
            // Set avatar skills
            avatar.getSkills().clear();
            for (var skillTreeData : avatarDetail.getSkillTreeList()) {
                avatar.getSkills().put(skillTreeData.getPointId(), skillTreeData.getLevel());
            }
            
            // Delete old relics/equips
            List<GameItem> unequipList = new ArrayList<>();
            
            // Force unequip all items
            int[] slots = avatar.getEquips().keySet().toIntArray();
            for (int slot : slots) {
                var item = avatar.unequipItem(slot);
                if (item != null) {
                    unequipList.add(item);
                }
            }
            
            if (args.hasFlag("-keepequips")) {
                // Update un-equipped items to the client
                player.sendPacket(new PacketPlayerSyncScNotify(unequipList));
            } else {
                // Delete old relics/equips
                player.getInventory().removeItems(unequipList);
            }
            
            // Set equipment
            var equipmentData = avatarDetail.getEquipment();
            if (equipmentData != null) {
                // Validate equipment excel (In case we are using an older version of the server)
                var excel = GameData.getItemExcelMap().get(equipmentData.getTid());
                if (excel != null) {
                    // Create equipment
                    var equipment = new GameItem(excel);
                    
                    // Set equipment props
                    equipment.setLevel(equipmentData.getLevel());
                    equipment.setExp(0);
                    equipment.setPromotion(equipmentData.getPromotion());
                    equipment.setRank(equipmentData.getRank());
                    
                    // Add to player
                    player.getInventory().addItem(equipment);
                    avatar.equipItem(equipment);
                }
            }
            
            // Set relics
            for (var relicData : avatarDetail.getRelicList()) {
                // Validate relic excel (In case we are using an older version of the server)
                var excel = GameData.getItemExcelMap().get(relicData.getTid());
                if (excel == null) continue;
                
                // Create relic
                var relic = new GameItem(excel);
                
                // Set relic props
                relic.setLevel(relicData.getLevel());
                relic.setExp(0);
                relic.setMainAffix(relicData.getMainAffixId());
                relic.resetSubAffixes();
                
                for (var subAffixData : relicData.getSubAffixList()) {
                    var subAffix = new GameItemSubAffix();
                    subAffix.setCount(subAffixData.getCnt());
                    subAffix.setStep(subAffixData.getStep());
                    
                    // Hacky way to set id field since its private
                    try {
                        Field field = subAffix.getClass().getDeclaredField("id");
                        field.setAccessible(true);
                        field.setInt(subAffix, subAffixData.getAffixId());
                    } catch (Exception e) {
                        // TODO handle
                    }
                    
                    relic.getSubAffixes().add(subAffix);
                }
                
                // Add to player
                player.getInventory().addItem(relic);
                avatar.equipItem(relic);
            }
            
            // Save
            avatar.save();
            player.sendPacket(new PacketPlayerSyncScNotify(avatar));
            
            // Increment count
            count++;
        }
        
        return count;
    }
}
