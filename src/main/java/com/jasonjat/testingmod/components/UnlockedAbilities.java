package com.jasonjat.testingmod.components;

import com.jasonjat.testingmod.Testingmod;
import com.jasonjat.testingmod.abilities.AbilityRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;
import static com.jasonjat.testingmod.Testingmod.LOGGER;

public class UnlockedAbilities implements AutoSyncedComponent, ServerTickingComponent {
    private final PlayerEntity player;
    private final List<Identifier> unlockedAbilities = new ArrayList<>();
    private final Map<Identifier, Integer> cooldowns = new LinkedHashMap<>();

    public UnlockedAbilities(PlayerEntity player) {
        this.player = player;
    }

    public boolean unlockAbility(String ability) {

        Identifier a = new Identifier(Testingmod.MOD_ID, ability);
        if (!unlockedAbilities.contains(a)) {
            return unlockedAbilities.add(a);
        }
        return false;
    }

    public boolean revokeAbility(String ability) {
        Identifier a = new Identifier(Testingmod.MOD_ID, ability);
        return unlockedAbilities.remove(a);
    }

    public void setCooldown(Identifier id, int cooldown) {
        if (cooldown > 0) {
            cooldowns.put(id, cooldown);
        } else {
            LOGGER.warn(id + " cooldown has to be over 0!");
        }
    }

    public int getCooldown(Identifier id) {
        if (cooldowns.containsKey(id)) {
            return cooldowns.get(id);
        }
        return 0;
    }

    public List<Identifier> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        unlockedAbilities.clear();

        NbtList nbtList = tag.getList("abilities", NbtType.STRING);
        nbtList.forEach(id -> unlockedAbilities.add(new Identifier(id.asString())));

        cooldowns.clear();
//        NbtList nbtKeyList = tag.getList("cooldownid", NbtType.STRING);
//        int[] nbtValueList = tag.getIntArray("cooldownvalue");
//        nbtKeyList.forEach(id-> {
//           for (int i = 0; i<nbtValueList.length; i++) {
//               cooldowns.put(new Identifier(id.asString()), nbtValueList[i]);
//           }
//        });

        NbtList hashList = tag.getList("cooldown", NbtType.COMPOUND);
        for (int i = 0; i<hashList.size(); i++) {
            NbtCompound hashTag = hashList.getCompound(i); //contains both the key and value
            Identifier id = Identifier.tryParse(hashTag.getString("cooldownid"));
            int cooldownV = hashTag.getInt("cooldownvalue");
            if (id != null) {
                cooldowns.put(id, cooldownV);
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList nbtList = new NbtList();

        unlockedAbilities.forEach(id -> nbtList.add(NbtString.of(id.toString())));
        tag.put("abilities", nbtList);

//        NbtList nbtKeyList = new NbtList();
//
//        for (Identifier id : cooldowns.keySet()) {
//            nbtKeyList.add(NbtString.of(id.toString()));
//        }
//
//        tag.put("cooldownid", nbtKeyList);
//
//        int[] intArray = cooldowns.values().stream().mapToInt(Integer::intValue).toArray();
//        NbtIntArray nbtIntArray = new NbtIntArray(intArray);

        NbtList nbtKeyList = new NbtList();
        for (Map.Entry<Identifier, Integer> entry : cooldowns.entrySet()) {
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putString("cooldownid", entry.getKey().toString());
            nbtCompound.putInt("cooldownvalue", entry.getValue());
            nbtKeyList.add(nbtCompound);
        }
        tag.put("cooldown", nbtKeyList);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player == player;
    }

    @Override //cool down handler
    public void serverTick() {
        if (player.age % 20 == 0) {
            for (Identifier id : unlockedAbilities) {
                if (cooldowns.containsKey(id)) {
                    int cooldown = cooldowns.get(id);
                    if (cooldown > 0) {
                        cooldowns.put(id, cooldown - 1);
                    }
                } else {
                    cooldowns.put(id, 0);
                }
            }
        }
        MyComponents.UNLOCKED_ABILITIES.sync(player);
    }
}
