package dev.melledy.importer.enka;

import java.util.List;

import lombok.Getter;

@Getter
public class EnkaUserData {
    private DetailInfo detailInfo;
    private int ttl;
    
    @Getter
    public static class DetailInfo {
        private String nickname;
        private long uid;
        private List<AvatarDetail> avatarDetailList;
    }
    
    @Getter
    public static class AvatarDetail {
        private int avatarId;
        private int level;
        private int promotion;
        private int rank;
        
        private EquipmentInfo equipment;
        private List<AvatarSkillTreeInfo> skillTreeList;
        private List<RelicInfo> relicList;
    }
    
    @Getter
    public static class AvatarSkillTreeInfo {
        private int pointId;
        private int level;
    }
    
    @Getter
    public static class EquipmentInfo {
        private int tid;
        private int level;
        private int promotion;
        private int rank;
    }
    
    @Getter
    public static class RelicInfo {
        private int tid;
        private int level;
        private int type;
        private int mainAffixId;
        private List<RelicSubAffix> subAffixList;
    }
    
    @Getter
    public static class RelicSubAffix {
        private int affixId;
        private int cnt;
        private int step;
    }
}
