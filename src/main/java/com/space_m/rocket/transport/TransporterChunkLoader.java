package com.space_m.rocket.transport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.space_m.platform.SavedDataHooks;
import com.space_m.platform.SpaceSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * 1:1 NTM Space {@code ChunkLoaderManager} (nur vom Transporter benutzt): ein Block erzwingt "seinen" Chunk, nur
 * dieser Block kann ihn wieder freigeben; der Chunk bleibt erzwungen, solange ein anderer Block derselben Liste darin
 * steht. Je Dimension gespeichert ({@code ChunkData} -> {@code space_m_chunk_data}).
 *
 * <p>Abweichung (technisch noetig): statt eines Forge-{@code Ticket} die Vanilla-Zwangsladung
 * {@link ServerLevel#setChunkForced} (beide Loader, persistent, beim Serverstart geladen). War ein Chunk schon vorher
 * erzwungen (z.B. {@code /forceload}), wird er beim Abbau nicht freigegeben ({@code preforced}).
 * Original-Klasse: ChunkLoaderManager
 */
public class TransporterChunkLoader extends SpaceSavedData {

    private static final String DATA_NAME = "space_m_chunk_data";

    /** Block -> Chunk (Original {@code chunksForcedBy}). */
    public final Map<BlockPos, ChunkPos> chunksForcedBy = new HashMap<>();
    /** Chunks, die vor uns schon erzwungen waren (nie freigeben). */
    private final Set<Long> preforced = new HashSet<>();

    private static TransporterChunkLoader get(ServerLevel level) {
        return SavedDataHooks.get(level, DATA_NAME, TransporterChunkLoader::new, TransporterChunkLoader::load);
    }

    /** Original {@code forceChunk(world, x, y, z)}: Chunk der Position erzwingen und dem Block zuordnen. */
    public static void forceChunk(ServerLevel level, BlockPos pos) {
        TransporterChunkLoader data = get(level);
        ChunkPos chunk = new ChunkPos(pos);
        boolean known = data.chunksForcedBy.containsValue(chunk);
        if (pos.equals(data.findOwner(pos, chunk)) && level.getForcedChunks().contains(chunk.toLong())) return;

        if (!known && level.getForcedChunks().contains(chunk.toLong())) data.preforced.add(chunk.toLong());
        data.chunksForcedBy.put(pos.immutable(), chunk);
        data.setDirty();

        level.setChunkForced(chunk.x, chunk.z, true);
    }

    /** Original {@code unforceChunk(world, x, y, z)}. */
    public static void unforceChunk(ServerLevel level, BlockPos pos) {
        TransporterChunkLoader data = get(level);
        ChunkPos chunk = data.chunksForcedBy.remove(pos);
        if (chunk == null) chunk = new ChunkPos(pos);
        data.setDirty();

        if (!data.chunksForcedBy.containsValue(chunk)) {
            if (!data.preforced.remove(chunk.toLong())) {
                level.setChunkForced(chunk.x, chunk.z, false);
            }
        }
    }

    /** true, wenn der Block seinen Chunk erzwungen hat (fuer GameTests). */
    public static boolean isForcedBy(ServerLevel level, BlockPos pos) {
        return get(level).chunksForcedBy.containsKey(pos);
    }

    private BlockPos findOwner(BlockPos pos, ChunkPos chunk) {
        return chunksForcedBy.containsKey(pos) && chunk.equals(chunksForcedBy.get(pos)) ? pos : null;
    }

    private static TransporterChunkLoader load(CompoundTag nbt) {
        TransporterChunkLoader data = new TransporterChunkLoader();
        ListTag list = nbt.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            data.chunksForcedBy.put(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    new ChunkPos(tag.getInt("cx"), tag.getInt("cz")));
        }
        for (long l : nbt.getLongArray("preforced")) data.preforced.add(l);
        return data;
    }

    @Override
    protected CompoundTag write(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, ChunkPos> entry : chunksForcedBy.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", entry.getKey().getX());
            tag.putInt("y", entry.getKey().getY());
            tag.putInt("z", entry.getKey().getZ());
            tag.putInt("cx", entry.getValue().x);
            tag.putInt("cz", entry.getValue().z);
            list.add(tag);
        }
        nbt.put("chunks", list);
        nbt.putLongArray("preforced", preforced.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }
}
