package dev.theredstonee.trsclient.core.terrain;

/** Lucerna/DH mini-IDs, not Minecraft registry IDs. Cache states remain unchanged. */
public final class TerrainMaterial {
    private TerrainMaterial() {}
    public static int classify(int state) {
        if(state<0)return 0;
        int id=state>>4;
        switch(id) {
            case 0:return 14;
            case 8:case 9:return 12;
            case 10:case 11:return 6;
            case 18:case 161:case 106:return 1;
            case 2:case 31:return 13;
            case 3:case 60:case 82:case 110:return 5;
            case 5:case 17:case 162:case 47:case 53:case 54:case 58:case 85:case 86:case 96:case 99:case 100:case 125:case 126:case 134:case 135:case 136:case 146:case 163:case 164:case 170:return 3;
            case 41:case 42:case 57:case 71:case 101:case 133:case 145:case 147:case 148:case 152:case 167:return 4;
            case 78:case 79:case 80:case 174:return 8;
            case 12:case 24:case 128:case 179:case 180:case 181:case 182:return 9;
            case 159:case 172:return 10;
            case 87:case 112:case 113:case 114:case 153:return 11;
            case 50:case 51:case 62:case 74:case 89:case 91:case 94:case 124:case 138:case 169:return 15;
            case 1:case 4:case 7:case 13:case 14:case 15:case 16:case 21:case 22:case 23:case 43:case 44:case 45:case 48:case 49:case 56:case 61:case 67:case 73:case 98:case 109:case 121:case 129:case 139:case 155:case 156:case 158:case 168:return 2;
            default:return 0;
        }
    }
}
